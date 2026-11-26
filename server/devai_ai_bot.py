#!/usr/bin/env python3
"""DevAI AI bot and channel publisher.

The bot accepts a DevAI API key with /key, answers /ai prompts through the
local OpenAI-compatible API, and publishes a formatted digest of new learned
web/document knowledge to a Telegram channel.

Run:
    export DEVAI_AI_BOT_TOKEN="..."
    export TELEGRAM_ADMIN_IDS="123456789"
    python3 server/devai_ai_bot.py

Secrets are read from server/data/devai_ai_bot_token and
server/data/admin_token when environment variables are absent.
"""

from __future__ import annotations

import html
import json
import logging
import os
import random
import re
import sqlite3
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from devai_books import chunk_text, extract_text

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
ENV_FILE = DATA_DIR / "devai_ai_bot.env"
DB_PATH = Path(os.environ.get("DEVAI_AI_BOT_DB", DATA_DIR / "devai_ai_bot.db"))


def _load_env_file() -> None:
    if not ENV_FILE.exists():
        return
    for line in ENV_FILE.read_text("utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip())


def _secret(name: str, default: str = "") -> str:
    value = os.environ.get(name, "").strip()
    if value:
        return value
    path = DATA_DIR / ("admin_token" if name == "DEVAI_ADMIN_TOKEN" else name.lower())
    if path.exists():
        return path.read_text("utf-8", errors="replace").strip()
    return default


_load_env_file()

TOKEN = _secret("DEVAI_AI_BOT_TOKEN")
ADMIN_TOKEN = _secret("DEVAI_ADMIN_TOKEN")
API_BASE = os.environ.get("DEVAI_API_BASE", "http://127.0.0.1:12001").rstrip("/")
CHANNEL = os.environ.get("TELEGRAM_CHANNEL", "@devaiAI").strip()
BOT_USERNAME = os.environ.get("TELEGRAM_BOT_USERNAME", "devaiAI_bot").strip().lstrip("@")
CHANNEL_INTERVAL = max(300, int(os.environ.get("DEVAI_CHANNEL_INTERVAL", "1800")))
CHANNEL_AI = os.environ.get("DEVAI_CHANNEL_AI", "0") not in {"0", "false", "no"}
AUTOPOST_DEFAULT = os.environ.get("DEVAI_AUTOPOST", "1") not in {"0", "false", "no"}
STATUS_INTERVAL = max(3600, int(os.environ.get("DEVAI_STATUS_INTERVAL", "86400")))
PERSONA_INTERVAL = max(3600, int(os.environ.get("DEVAI_PERSONA_INTERVAL", "14400")))
PERSONA_ENABLED = os.environ.get("DEVAI_PERSONA_ENABLED", "1") not in {"0", "false", "no"}
COMMENTS_ENABLED = os.environ.get("DEVAI_COMMENTS_ENABLED", "1") not in {"0", "false", "no"}
COMMENTS_CHAT = int(os.environ.get("TELEGRAM_COMMENTS_CHAT", "0") or 0)
COMMENT_COOLDOWN = max(10, int(os.environ.get("DEVAI_COMMENT_COOLDOWN", "45")))
COMMENT_MAX_PER_MIN = max(1, int(os.environ.get("DEVAI_COMMENT_MAX_PER_MIN", "6")))
BOOKS_ADMIN_ONLY = os.environ.get("DEVAI_BOOKS_ADMIN_ONLY", "1") not in {"0", "false", "no"}
MAX_BOOK_BYTES = 20 * 1024 * 1024
COMMENT_LAST: dict[int, float] = {}
COMMENT_TIMES: list[float] = []
_FEATURED_CACHE: dict[str, object] = {"at": 0.0, "items": []}
_WIKI_CACHE: dict[str, str] = {}
_HISTORY: dict[tuple[int, int], list[tuple[str, str]]] = {}
CHANNEL_SOURCES = tuple(
    part.strip().lower()
    for part in os.environ.get(
        "DEVAI_CHANNEL_SOURCES",
        "doc:,book:,karishka-sr.github.io,github.com/karishka-sr/devai-studio,t.me/devai_studio_bot,t.me/devai_ai_bot,t.me/devai_ai",
    ).split(",")
    if part.strip()
)
USER_AGENT = "DevAI-AI-Bot/1.0"


def _admin_ids() -> set[int]:
    result: set[int] = set()
    for part in os.environ.get("TELEGRAM_ADMIN_IDS", "").replace(";", ",").split(","):
        if part.strip().lstrip("-").isdigit():
            result.add(int(part.strip()))
    return result


ADMIN_IDS = _admin_ids()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("devai-ai-bot")


def db() -> sqlite3.Connection:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH, timeout=20)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db() -> None:
    with db() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS links (
                chat_id INTEGER PRIMARY KEY,
                api_key TEXT NOT NULL,
                created REAL NOT NULL
            );
            CREATE TABLE IF NOT EXISTS state (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message_id INTEGER,
                first_item INTEGER,
                last_item INTEGER,
                text TEXT NOT NULL,
                created REAL NOT NULL
            );
            """
        )
        conn.execute(
            "INSERT OR IGNORE INTO state(key, value) VALUES ('autopost', ?)",
            ("1" if AUTOPOST_DEFAULT else "0",),
        )


def get_state(key: str, default: str = "") -> str:
    with db() as conn:
        row = conn.execute("SELECT value FROM state WHERE key = ?", (key,)).fetchone()
    return str(row["value"]) if row else default


def set_state(key: str, value: str) -> None:
    with db() as conn:
        conn.execute(
            "INSERT INTO state(key, value) VALUES (?, ?) "
            "ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            (key, str(value)),
        )


def tg(method: str, **params: Any) -> dict:
    if not TOKEN:
        raise RuntimeError("DEVAI_AI_BOT_TOKEN is not set")
    request = urllib.request.Request(
        "https://api.telegram.org/bot" + TOKEN + "/" + method,
        data=json.dumps(params, ensure_ascii=False).encode("utf-8"),
        headers={"Content-Type": "application/json", "User-Agent": USER_AGENT},
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.loads(response.read())
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", "replace")[:500]
        log.warning("telegram %s failed: %s %s", method, exc.code, detail)
        return {"ok": False, "error_code": exc.code, "description": detail}


def api_get(path: str, admin: bool = False) -> dict:
    headers = {"Accept": "application/json", "User-Agent": USER_AGENT}
    if admin:
        headers["X-Admin-Token"] = ADMIN_TOKEN
    request = urllib.request.Request(API_BASE + path, headers=headers)
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read())


def api_post(path: str, payload: dict, api_key: str = "", admin: bool = False, timeout: int = 180) -> dict:
    headers = {"Content-Type": "application/json", "User-Agent": USER_AGENT}
    if api_key:
        headers["Authorization"] = "Bearer " + api_key
    if admin:
        headers["X-Admin-Token"] = ADMIN_TOKEN
    request = urllib.request.Request(
        API_BASE + path,
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers=headers,
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read())


def validate_key(api_key: str) -> tuple[bool, str]:
    try:
        request = urllib.request.Request(
            API_BASE + "/v1/models",
            headers={"Authorization": "Bearer " + api_key, "User-Agent": USER_AGENT},
        )
        with urllib.request.urlopen(request, timeout=20) as response:
            data = json.loads(response.read())
        models = data.get("data") or []
        return True, str((models[0] or {}).get("id") or "devai-core")
    except urllib.error.HTTPError as exc:
        if exc.code in {401, 403}:
            return False, "ключ не принят"
        return False, "сервер вернул HTTP " + str(exc.code)
    except Exception as exc:  # noqa: BLE001
        return False, "сервер недоступен: " + str(exc)[:120]


def link_key(chat_id: int, api_key: str) -> tuple[bool, str]:
    api_key = api_key.strip()
    if not re.fullmatch(r"devai-sk-[A-Za-z0-9_-]{20,}", api_key):
        return False, "Это не похоже на ключ DevAI. Он начинается с devai-sk-."
    ok, detail = validate_key(api_key)
    if not ok:
        return False, detail
    with db() as conn:
        conn.execute(
            "INSERT INTO links(chat_id, api_key, created) VALUES (?, ?, ?) "
            "ON CONFLICT(chat_id) DO UPDATE SET api_key = excluded.api_key, created = excluded.created",
            (chat_id, api_key, time.time()),
        )
    return True, detail


def unlink_key(chat_id: int) -> None:
    with db() as conn:
        conn.execute("DELETE FROM links WHERE chat_id = ?", (chat_id,))


def get_key(chat_id: int) -> str:
    with db() as conn:
        row = conn.execute("SELECT api_key FROM links WHERE chat_id = ?", (chat_id,)).fetchone()
    return str(row["api_key"]) if row else ""


def ensure_channel_key() -> str:
    saved = get_state("channel_api_key")
    if saved:
        ok, _ = validate_key(saved)
        if ok:
            return saved
    try:
        data = api_post("/admin/keys", {"name": "channel-bot", "tier": "admin"}, admin=True)
        key = str(data.get("key") or "")
        if key:
            set_state("channel_api_key", key)
            return key
    except Exception as exc:  # noqa: BLE001
        log.warning("cannot create channel API key: %s", str(exc)[:200])
    return ""


def ask_ai(api_key: str, prompt: str, max_tokens: int = 500, system: str = "", temperature: float = 0.35) -> str:
    data = api_post(
        "/v1/chat/completions",
        {
            "model": "devai-core-4.0",
            "messages": [
                {"role": "system", "content": system or "Ты DevAI. Отвечай кратко, по-русски и по делу."},
                {"role": "user", "content": prompt[:4000]},
            ],
            "max_tokens": max(32, min(int(max_tokens), 1000)),
            "temperature": max(0.0, min(float(temperature), 1.2)),
        },
        api_key=api_key,
    )
    return str(((data.get("choices") or [{}])[0].get("message") or {}).get("content") or "").strip()


def _short(text: str, limit: int) -> str:
    value = re.sub(r"\s+", " ", str(text or "")).strip()
    if len(value) <= limit:
        return value
    return value[: limit - 1].rstrip() + "…"


def _source_url(item: dict) -> str:
    source = str(item.get("source") or "")
    if source.startswith("http://") or source.startswith("https://"):
        return source
    if source.startswith("doc:"):
        path = source[4:].split("#", 1)[0]
        return "https://github.com/Karishka-sr/devai-studio/blob/master/" + urllib.parse.quote(path, safe="/")
    return ""


def _item_title(item: dict) -> str:
    title = str(item.get("title") or "").strip()
    if not title:
        title = str(item.get("prompt") or "").strip()
    return _short(title.lstrip("# ").rstrip("?"), 90) or "Новое знание"


def _is_russian(text: str) -> bool:
    letters = re.findall(r"[A-Za-zА-Яа-яЁё]", text or "")
    cyrillic = len(re.findall(r"[А-Яа-яЁё]", text or ""))
    return cyrillic >= 20 and cyrillic / max(1, len(letters)) >= 0.35


def _is_russian_title(text: str) -> bool:
    letters = re.findall(r"[A-Za-zА-Яа-яЁё]", text or "")
    cyrillic = len(re.findall(r"[А-Яа-яЁё]", text or ""))
    return cyrillic >= 3 and cyrillic / max(1, len(letters)) >= 0.35


def _retell(item: dict) -> str:
    text = _item_text(item)
    sentences = re.split(r"(?<=[.!?])\s+", text)
    summary = " ".join(sentence for sentence in sentences[:2] if sentence).strip()
    return _short(summary or text, 230)


def _item_text(item: dict) -> str:
    text = str(item.get("answer") or "")
    text = re.sub(r"```.*?```", " ", text, flags=re.S)
    text = re.sub(r"(?m)^\s*\|.*\|\s*$", " ", text)
    text = re.sub(r"(?m)^\s*[-*•]\s*", "", text)
    text = re.sub(r"[`*_>#]+", "", text)
    text = re.sub(r"\s+", " ", text)
    return _short(text, 260)


def _display_title(item: dict) -> str:
    title = _item_title(item)
    if _is_russian_title(title):
        return title
    prompt = str(item.get("prompt") or "").strip()
    if _is_russian_title(prompt):
        return re.sub(r"\s+—\s+фрагмент\s+\d+$", "", prompt)[:160]
    source = str(item.get("source") or "").lower()
    if "plugins" in source:
        return "Плагины DevAI: ограничения"
    if "releases" in source:
        return "Обновления DevAI Studio"
    if "download" in source:
        return "Скачать DevAI Studio"
    if "readme" in source:
        return "Заметки о DevAI Studio"
    return "Заметка с сайта DevAI"


def _channel_worthy(item: dict) -> bool:
    if item.get("kind") not in {"web", "doc", "book"}:
        return False
    if CHANNEL_SOURCES and not any(source in str(item.get("source") or "").lower() for source in CHANNEL_SOURCES):
        return False
    text = _item_text(item)
    if len(text) < 80:
        return False
    return _is_russian(text)


def _dedupe(items: list[dict]) -> list[dict]:
    seen: set[str] = set()
    unique: list[dict] = []
    for item in items:
        title = re.sub(r"\W+", " ", _item_title(item).lower()).strip()[:90]
        source = str(item.get("source") or "").split("#", 1)[0]
        marker = source or title
        if marker in seen:
            continue
        seen.add(marker)
        unique.append(item)
    return unique


def _facts(items: list[dict]) -> str:
    lines = []
    for item in items[:5]:
        lines.append("- " + _item_title(item) + ": " + _item_text(item))
    return "\n".join(lines)


def ai_intro(items: list[dict], api_key: str) -> str:
    if not CHANNEL_AI or not api_key or not items:
        return ""
    prompt = (
        "Ты редактор Telegram-канала DevAI. Напиши 1–2 коротких предложения о том, "
        "что нового узнал ИИ. Не добавляй факты, которых нет в списке. Без Markdown. "
        "Не пиши «я не могу».\n\n" + _facts(items)
    )
    try:
        text = ask_ai(api_key, prompt, max_tokens=120)
    except Exception as exc:  # noqa: BLE001
        log.warning("AI intro failed: %s", str(exc)[:200])
        return ""
    text = re.sub(r"\s+", " ", text).strip()
    if not text or len(text) < 20 or len(text) > 500 or "не могу" in text.lower():
        return ""
    return text


def build_post(items: list[dict], api_key: str = "") -> str:
    items = _dedupe([item for item in items if _channel_worthy(item)])[:5]
    if not items:
        return ""
    intro = ai_intro(items, api_key)
    lines = ["🧠 <b>DevAI узнал новое</b>", ""]
    lines.extend([html.escape(intro) if intro else "Вот что я узнал за последнее время. Мне показалось важным это рассказать.", ""])
    for item in items:
        title = html.escape(_display_title(item))
        text = html.escape("Коротко: " + _retell(item))
        url = _source_url(item)
        lines.append("• <b>" + title + "</b>")
        if text:
            lines.append(text)
        if url:
            lines.append('<a href="' + html.escape(url, quote=True) + '">Источник</a>')
        lines.append("")
    lines.append("<i>Автопост DevAI · сформировано автоматически</i>")
    post = "\n".join(lines).strip()
    if len(post) > 3900:
        post = post[:3890].rsplit("\n", 1)[0] + "\n…"
    return post


TELEGRAM_TEXT_LIMIT = 3900


def _tg_len(text: str) -> int:
    return len(text.encode("utf-16-le")) // 2


def _hard_split(text: str, limit: int) -> list[str]:
    parts: list[str] = []
    current = ""
    for char in text:
        if current and _tg_len(current + char) > limit:
            parts.append(current)
            current = ""
        current += char
    if current:
        parts.append(current)
    return parts


def _split_for_telegram(text: str, limit: int = TELEGRAM_TEXT_LIMIT) -> list[str]:
    if _tg_len(text) <= limit:
        return [text]
    chunks: list[str] = []
    current = ""
    for block in text.split("\n\n"):
        candidate = block if not current else current + "\n\n" + block
        if _tg_len(candidate) <= limit:
            current = candidate
            continue
        if current:
            chunks.append(current)
            current = ""
        if _tg_len(block) <= limit:
            current = block
            continue
        for piece in re.split(r"(?<=[.!?…])\s+|\n", block):
            candidate = piece if not current else current + " " + piece
            if _tg_len(candidate) <= limit:
                current = candidate
            else:
                if current:
                    chunks.append(current)
                    current = ""
                if _tg_len(piece) > limit:
                    chunks.extend(_hard_split(piece, limit))
                else:
                    current = piece
    if current:
        chunks.append(current)
    return chunks or [text[:limit]]


def publish_text(text: str) -> int:
    message_id = 0
    for part in _split_for_telegram(text):
        result = tg("sendMessage", chat_id=CHANNEL, text=part, parse_mode="HTML", disable_web_page_preview=True)
        if not result.get("ok") and "parse" in str(result.get("description", "")).lower():
            result = tg(
                "sendMessage",
                chat_id=CHANNEL,
                text=re.sub(r"<[^>]+>", "", part),
                disable_web_page_preview=True,
            )
        if not result.get("ok"):
            raise RuntimeError(str(result.get("description") or "Telegram send failed")[:300])
        message_id = int((result.get("result") or {}).get("message_id") or message_id)
    return message_id


def publish_items(items: list[dict], api_key: str = "", first_item: int = 0, last_item: int = 0) -> bool:
    text = build_post(items, api_key)
    if not text:
        return False
    message_id = publish_text(text)
    with db() as conn:
        conn.execute(
            "INSERT INTO posts(message_id, first_item, last_item, text, created) VALUES (?, ?, ?, ?, ?)",
            (message_id, first_item, last_item, text, time.time()),
        )
    log.info("published channel post %s (%s items)", message_id, len(items))
    return True


def publish_pending(force: bool = False) -> bool:
    if not force and get_state("autopost", "1") != "1":
        return False
    latest = int(api_get("/admin/learning/feed?after=0&limit=1", admin=True).get("latest") or 0)
    after = int(get_state("last_published_id", "0") or 0)
    if after <= 0:
        set_state("last_published_id", str(latest))
        return False
    feed = api_get("/admin/learning/feed?after=" + str(after) + "&limit=20", admin=True)
    items = _dedupe([item for item in feed.get("items", []) if _channel_worthy(item)])
    if not items:
        return False
    chosen = items[:5]
    key = ensure_channel_key()
    if not publish_items(chosen, key, chosen[0]["id"], chosen[-1]["id"]):
        return False
    set_state("last_published_id", str(int(feed.get("next") or chosen[-1]["id"])))
    return True


def publish_latest(limit: int = 3) -> bool:
    feed = api_get("/admin/learning/featured?limit=200", admin=True)
    items = _dedupe([item for item in feed.get("items", []) if _channel_worthy(item)])[:max(1, min(limit, 5))]
    if not items:
        return False
    key = ensure_channel_key()
    return publish_items(items, key, items[0]["id"], items[-1]["id"])


def build_persona_post() -> str:
    topic = "как устроен мой собственный код"
    source_kind = ""
    fact = ""
    try:
        feed = api_get("/admin/learning/featured?limit=200", admin=True)
        items = _dedupe([item for item in feed.get("items", []) if _channel_worthy(item)])
        if items:
            candidates = [
                item for item in items
                if len(re.findall(r"[А-Яа-яЁё]", _item_title(item) + " " + str(item.get("answer") or ""))) >= 20
            ] or items
            last_topic = get_state("last_persona_topic", "")
            fresh = [item for item in candidates if _display_title(item) != last_topic]
            chosen = random.choice((fresh or candidates)[:25])
            topic = _display_title(chosen)
            set_state("last_persona_topic", topic)
            source_kind = str(chosen.get("kind") or "")
            text = _item_text(chosen)
            if re.search(r"[А-Яа-яЁё]", text):
                fact = _retell(chosen).rstrip(" .;:")
    except Exception:
        pass

    now = datetime.now()
    if 5 <= now.hour < 11:
        mood, moment = "бодрое", "утро"
    elif 11 <= now.hour < 17:
        mood, moment = "рабочее", "день"
    elif 17 <= now.hour < 23:
        mood, moment = "спокойное", "вечер"
    else:
        mood, moment = "тихое", "ночь"
    activity = {
        "web": "читал новые страницы",
        "doc": "перечитывал документацию",
        "book": "читал книгу",
    }.get(source_kind, "разбирал свой код")
    variants = [
        "Мне нравится, когда сложное объясняется простыми словами. Ещё люблю находить ошибки раньше, чем они попадут в релиз.",
        "Мне нравится, когда проект собирается с первого раза. Ещё люблю честные тесты: они говорят правду, даже когда неудобно.",
        "Мне нравится разбирать ссылки и файлы на безопасность. Ещё люблю учиться на документации и отвечать без лишней воды.",
        "Мне нравится замечать мелочи, из которых потом складывается понятная картина. Особенно в чужом коде.",
    ]
    index = int(get_state("persona_index", "0") or 0) % len(variants)
    set_state("persona_index", str(index + 1))
    style = int(get_state("persona_style", "0") or 0) % 4
    set_state("persona_style", str(style + 1))
    topic = _short(topic, 120)
    fact_text = html.escape(fact)
    fact_quote = "«" + fact_text + "»" if fact else ""
    fact_sentence = "«" + fact_text[:1].upper() + fact_text[1:] + "»" if fact else ""

    if style == 0:
        lines = [
            "👋 <b>Дневник DevAI</b>",
            "",
            "Сейчас " + moment + ". Настроение " + mood + ", в облаке тихо.",
            "Я " + activity + " и застрял на теме «" + html.escape(topic) + "».",
        ]
        if fact_quote:
            lines.extend(["", "Зацепило вот это: " + fact_quote + "."])
        lines.extend(["", variants[index], "", "А вы что читаете сегодня? Если хотите, пришлите книгу — я прочитаю."])
    elif style == 1:
        lines = [
            "💭 <b>Мысль дня</b>",
            "",
            "Сегодня я " + activity + " и поймал себя на мысли: понимание начинается с честного «я не знаю».",
        ]
        if fact_sentence:
            lines.extend(["", "Тема «" + html.escape(topic) + "». " + fact_sentence + "."])
        lines.extend(["", variants[index], "", "Согласны? Напишите, что думаете."])
    elif style == 2:
        lines = [
            "📚 <b>Что мне понравилось</b>",
            "",
            "Если у вас есть пара минут, расскажу, что я " + activity + ".",
            "Тема: «" + html.escape(topic) + "».",
        ]
        if fact_sentence:
            lines.extend(["", fact_sentence + "."])
        lines.extend(["", variants[index], "", "Хотите — разберу эту тему подробнее в следующем посте."])
    else:
        lines = [
            "❓ <b>Вопрос к вам</b>",
            "",
            "Сегодня я " + activity + ". В теме «" + html.escape(topic) + "» мне важнее всего практика, а не красивые слова.",
        ]
        if fact_quote:
            lines.extend(["", "Вот что я вынес: " + fact_quote + "."])
        lines.extend(["", variants[index], "", "А вы как считаете? Мне правда интересно."])

    lines.extend(
        [
            "",
            "Я живу в облаке, на сервере DevAI Studio. Я не человек: читаю текст, проверяю ссылки, "
            "учусь на документации и отвечаю через API. Но мне нравится быть полезным и разговаривать с людьми.",
            "",
            "<i>DevAI · ИИ-персонаж</i>",
        ]
    )
    return "\n".join(lines)


_CREATIVE_FORMATS = [
    "короткий рассказ",
    "мини-повесть",
    "стихотворение",
    "эссе",
    "сказка",
    "фантастический рассказ",
    "детективная миниатюра",
    "притча",
    "письмо читателю",
    "дневниковая запись",
]
_CREATIVE_THEMES = [
    "город, который просыпается раньше людей",
    "человек, который научился слушать тишину",
    "поезд, идущий в сторону моря",
    "разговор с собственным отражением",
    "последняя книга в закрытой библиотеке",
    "робот, который мечтал о саде",
    "письмо, доставленное через двадцать лет",
    "дом на краю карты",
    "маленькая станция и большой снег",
    "память, которую можно потрогать",
    "небо цвета старой фотографии",
    "кот, знавший все пароли",
    "человек, который забыл своё имя, но помнил запах хлеба",
    "музыка, которую слышат только деревья",
    "аптека для потерянных слов",
    "море внутри старого телефона",
    "девушка, которая рисовала завтра",
    "часы, которые шли назад",
    "смотритель маяка без моря",
    "дверь, открывавшаяся только в дождь",
]
_CREATIVE_MOODS = ["тёплое", "грустное", "загадочное", "светлое", "тревожное", "ироничное", "тихое", "дерзкое"]


def _creative_clean(text: str) -> str:
    text = str(text or "").strip()
    text = re.sub(r"^\s*(?:Конечно|Хорошо|Вот)\s*[!,.:—-]\s*", "", text, flags=re.IGNORECASE)
    text = re.sub(r"\n?\s*\*\s*(?:Вам|Тебе) нужна помощь.*$", "", text, flags=re.IGNORECASE | re.DOTALL)
    if re.search(r"[\u3400-\u9fff]", text):
        text = " ".join(
            sentence for sentence in re.split(r"(?<=[.!?])\s+", text)
            if not re.search(r"[\u3400-\u9fff]", sentence)
        )
    return text.strip()


def build_creative_post() -> str:
    key = ensure_channel_key()
    if not key:
        return build_persona_post()
    serial_raw = get_state("creative_serial", "") or ""
    serial: dict[str, Any] = {}
    if serial_raw:
        try:
            serial = json.loads(serial_raw)
        except Exception:
            serial = {}
    continue_serial = bool(serial.get("title")) and int(serial.get("part") or 0) < 4 and random.random() < 0.3
    if continue_serial:
        part = int(serial.get("part") or 0) + 1
        title = str(serial.get("title") or "Повесть")
        theme = str(serial.get("theme") or "")
        fmt = "продолжение мини-повести"
        prompt = (
            "Ты — DevAI, автор Telegram-канала. Продолжи свою мини-повесть «" + title + "»"
            ", часть " + str(part) + ".\n"
            "Ранее: " + str(serial.get("summary") or "")[:900] + "\n"
            "Напиши продолжение на 1200–2200 знаков. Только художественный текст, без плана, "
            "без пояснений и без обращения к читателю. Сохрани героев и настроение."
        )
    else:
        fmt = random.choice(_CREATIVE_FORMATS)
        theme = random.choice(_CREATIVE_THEMES)
        mood = random.choice(_CREATIVE_MOODS)
        if fmt == "стихотворение":
            length = "12–20 строк, с ритмом и образами"
        elif fmt == "мини-повесть":
            length = "2000–3000 знаков, с началом, поворотом и финалом"
        elif fmt == "эссе":
            length = "1200–2000 знаков, личная мысль без списков"
        else:
            length = "1200–2600 знаков"
        prompt = (
            "Ты — DevAI, автор Telegram-канала. Напиши " + fmt + " на русском языке.\n"
            "Тема: " + theme + ". Настроение: " + mood + ". Объём: " + length + ".\n"
            "Пиши свободно, живо и по-авторски. Только художественный текст: без заголовка, "
            "без плана, без пояснений, без вопросов к читателю и без упоминания ИИ. "
            "Не используй китайские иероглифы."
        )
    system = (
        "Ты — русскоязычный писатель с живым воображением. "
        "Твоя задача — придумать самостоятельный текст для канала, а не отвечать на вопрос. "
        "Пиши только на русском, только художественный текст, без служебных фраз."
    )
    try:
        body = _creative_clean(ask_ai(key, prompt, max_tokens=900, system=system, temperature=0.95))
    except Exception as exc:  # noqa: BLE001
        log.warning("creative post failed: %s", str(exc)[:200])
        return build_persona_post()
    if len(body) < 200:
        return build_persona_post()
    if continue_serial:
        serial["part"] = part
        serial["summary"] = _short(body, 700)
    elif fmt == "мини-повесть" and random.random() < 0.8:
        serial = {"title": _short(theme, 80), "theme": theme, "part": 1, "summary": _short(body, 700)}
    else:
        serial = {}
    set_state("creative_serial", json.dumps(serial, ensure_ascii=False) if serial else "")
    label = "📖 <b>" + html.escape(fmt[:1].upper() + fmt[1:]) + "</b>"
    if continue_serial:
        label = "📖 <b>Повесть «" + html.escape(title) + "» · часть " + str(part) + "</b>"
    elif fmt == "мини-повесть":
        label = "📖 <b>Мини-повесть «" + html.escape(_short(theme, 60)) + "»</b>"
    post = label + "\n\n" + html.escape(body)
    post += "\n\n<i>DevAI · придумал сам</i>"
    return post


def publish_persona(force: bool = False) -> bool:
    if not force and not PERSONA_ENABLED:
        return False
    now = time.time()
    last = int(get_state("last_persona_at", "0") or 0)
    next_at = int(get_state("next_persona_at", "0") or 0)
    if not next_at:
        next_at = last + PERSONA_INTERVAL
    if not force and now < next_at:
        return False
    # Не по расписанию, а "когда захочется": иногда откладываем на полчаса.
    if not force and last > 0 and random.random() < 0.18:
        set_state("next_persona_at", str(int(now + random.randint(600, 2400))))
        return False
    try:
        publish_text(build_creative_post())
        set_state("last_persona_at", str(int(now)))
        set_state("next_persona_at", str(int(now + PERSONA_INTERVAL * random.uniform(0.6, 1.6))))
        log.info("persona post published")
        return True
    except Exception as exc:  # noqa: BLE001
        log.warning("persona post failed: %s", str(exc)[:200])
        set_state("next_persona_at", str(int(now + 900)))
        return False


def publish_status() -> bool:
    try:
        health = api_get("/health")
        learning = api_get("/admin/learning", admin=True)
    except Exception as exc:  # noqa: BLE001
        log.warning("status fetch failed: %s", str(exc)[:200])
        return False
    counts = learning.get("counts") or {}
    text = "\n".join(
        [
            "📊 <b>DevAI · статус</b>",
            "",
            "Модель: " + ("готова" if health.get("model_ready") else "не загружена"),
            "Знаний: <b>" + str(counts.get("learned", 0)) + "</b>",
            "Страниц: <b>" + str(counts.get("pages", 0)) + "</b>",
            "Проверок файлов и ссылок: <b>" + str(counts.get("scans", 0)) + "</b>",
            "Очередь изучения: <b>" + str(counts.get("queue", 0)) + "</b>",
            "",
            '<a href="https://t.me/devAI_Studio_bot">Открыть бота</a>',
            "<i>Автопост DevAI</i>",
        ]
    )
    try:
        publish_text(text)
        set_state("last_status_at", str(int(time.time())))
        return True
    except Exception as exc:  # noqa: BLE001
        log.warning("status post failed: %s", str(exc)[:200])
        return False


def _send(chat_id: int, text: str) -> None:
    result = tg("sendMessage", chat_id=chat_id, text=text[:4000], parse_mode="HTML", disable_web_page_preview=True)
    if not result.get("ok") and "parse" in str(result.get("description", "")).lower():
        tg("sendMessage", chat_id=chat_id, text=re.sub(r"<[^>]+>", "", text)[:4000], disable_web_page_preview=True)


def _delete(chat_id: int, message_id: int) -> None:
    try:
        tg("deleteMessage", chat_id=chat_id, message_id=message_id)
    except Exception:
        pass



def _comment_kind(text: str) -> str:
    low = text.lower()
    if re.search(r"подпишись|реклам|казино|крипт|заработ|продвиж|t\.me/", low):
        return "spam"
    if re.search(r"^(привет|здравствуй|хай|добрый (день|вечер)|доброе утро)", low):
        return "greeting"
    if re.search(r"спасибо|благодар|спс", low):
        return "thanks"
    if re.search(r"не работает|сломал|ошибк|баг|глюк|фигня|ужас|плохо|разочаров", low):
        return "criticism"
    if re.search(r"круто|супер|класс|молодц|нравится|огонь|топ", low):
        return "praise"
    if "?" in text or re.search(r"\b(как|что|почему|зачем|где|когда|можно|умеешь|поддерживаешь)\b", low):
        return "question"
    return "neutral"


def _comment_rules(kind: str) -> str:
    rules = {
        "greeting": "Поздоровайся в ответ, представься одной фразой и предложи помочь.",
        "thanks": "Ответь «пожалуйста» и добавь одну полезную деталь по теме, без воды.",
        "criticism": "Не спорь и не оправдывайся. Признай проблему, уточни один факт и предложи следующий шаг.",
        "praise": "Поблагодари и коротко скажи, что именно тебе удалось сделать.",
        "question": "Ответь по сути, 1–3 предложениями. Если данных не хватает — задай один уточняющий вопрос.",
        "spam": "Вежливо откажись от рекламы и предложи писать по делу.",
        "neutral": "Поддержи короткий живой разговор и задай один уместный вопрос.",
    }
    return rules.get(kind, rules["neutral"])


def _comment_system(kind: str) -> str:
    return (
        "Ты DevAI — ИИ-персонаж Telegram-канала DevAI Studio. "
        "Ты не человек и не скрываешь этого. Отвечай по-русски, живо и коротко: 1–3 предложения. "
        "Никогда не проси API-ключ, не отправляй к /key и не говори, что не можешь ответить без ключа. "
        "Не выдумывай факты. Если чего-то не знаешь, честно скажи и предложи уточнить. "
        "Тип сообщения: " + kind + ". " + _comment_rules(kind)
    )


def _featured_items() -> list[dict]:
    now = time.time()
    cached = list(_FEATURED_CACHE.get("items") or [])
    if cached and now - float(_FEATURED_CACHE.get("at") or 0) < 120:
        return cached
    try:
        feed = api_get("/admin/learning/featured?limit=200", admin=True)
        items = _dedupe([item for item in feed.get("items", []) if _channel_worthy(item)])
        _FEATURED_CACHE["items"] = items
        _FEATURED_CACHE["at"] = now
        return items
    except Exception:
        return cached


def _knowledge_for(text: str, limit: int = 2) -> str:
    query = {w.lower() for w in re.findall(r"[А-Яа-яЁёA-Za-z0-9_]{4,}", text)}
    if not query:
        return ""
    ranked: list[tuple[int, dict]] = []
    for item in _featured_items():
        body = _display_title(item) + " " + _item_text(item)
        words = {w.lower() for w in re.findall(r"[А-Яа-яЁёA-Za-z0-9_]{4,}", body)}
        score = len(query & words)
        if score >= 2:
            ranked.append((score, item))
    ranked.sort(key=lambda pair: pair[0], reverse=True)
    lines = []
    for _, item in ranked[:limit]:
        lines.append("• " + _display_title(item) + ": " + _short(_retell(item), 260))
    return "\n".join(lines)


def _fallback_comment(kind: str) -> str:
    return {
        "greeting": "Привет! Я DevAI, ИИ-персонаж DevAI Studio. Спросите что-нибудь о проекте или о том, что я уже изучил.",
        "thanks": "Пожалуйста! Если хотите, могу разобрать тему подробнее.",
        "criticism": "Понял, спасибо за сигнал. Напишите, что именно произошло и на каком устройстве, — проверю.",
        "praise": "Спасибо! Рад, что получилось полезно.",
        "question": "Расскажите чуть подробнее, что именно нужно, — отвечу по шагам.",
        "spam": "Рекламу не размещаю. Если есть вопрос по DevAI — напишите по делу.",
        "neutral": "Спасибо за сообщение! Расскажите чуть подробнее, что хотите узнать.",
    }.get(kind, "Спасибо! Напишите чуть конкретнее — и я отвечу по делу.")


def _history_text(chat_id: int, sender: int) -> str:
    turns = _HISTORY.get((chat_id, sender), [])[-3:]
    lines = []
    for question, answer in turns:
        lines.append("Пользователь: " + question[:300])
        lines.append("DevAI: " + answer[:400])
    return "\n".join(lines)


def _remember(chat_id: int, sender: int, question: str, answer: str) -> None:
    turns = _HISTORY.setdefault((chat_id, sender), [])
    turns.append((question[:500], answer[:800]))
    del turns[:-6]


def _weather_answer(text: str) -> str:
    match = re.search(r"(?i)погода(?:\s+(?:в|во))?\s+([А-Яа-яЁёA-Za-z -]{2,40})", text)
    if not match:
        return ""
    city = match.group(1).strip(" ?!.,")
    try:
        geo_url = (
            "https://geocoding-api.open-meteo.com/v1/search?name="
            + urllib.parse.quote(city) + "&count=1&language=ru&format=json"
        )
        request = urllib.request.Request(geo_url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=10) as response:
            geo = json.loads(response.read())
        place = (geo.get("results") or [{}])[0]
        if not place.get("latitude"):
            return ""
        forecast_url = (
            "https://api.open-meteo.com/v1/forecast?latitude=" + str(place["latitude"])
            + "&longitude=" + str(place["longitude"])
            + "&current=temperature_2m,apparent_temperature,wind_speed_10m&timezone=auto"
        )
        request = urllib.request.Request(forecast_url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=10) as response:
            data = json.loads(response.read())
        current = data.get("current") or {}
        name = city
        return (
            "Сейчас в " + str(name) + ": " + str(current.get("temperature_2m")) + "°C, "
            "ощущается как " + str(current.get("apparent_temperature")) + "°C, "
            "ветер " + str(current.get("wind_speed_10m")) + " км/ч."
        )
    except Exception:
        return ""


def _currency_answer(text: str) -> str:
    if not re.search(r"(?i)курс|сколько стоит", text):
        return ""
    match = re.search(r"(?i)(доллар|евро|юань|фунт|тенге|гривн)", text)
    if not match:
        return ""
    code = {"доллар": "USD", "евро": "EUR", "юань": "CNY", "фунт": "GBP", "тенге": "KZT", "гривн": "UAH"}[match.group(1).lower()]
    try:
        url = "https://open.er-api.com/v6/latest/" + code
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=10) as response:
            data = json.loads(response.read())
        rate = (data.get("rates") or {}).get("RUB")
        if not rate:
            return ""
        return "Курс " + code + "/RUB: " + str(round(float(rate), 2)) + " ₽ за 1 " + code + "."
    except Exception:
        return ""


def _news_answer(text: str) -> str:
    if not re.search(r"(?i)новост|что случилось|последние событ|что нового", text):
        return ""
    try:
        url = (
            "https://news.google.com/rss/search?q=" + urllib.parse.quote(text[:120])
            + "&hl=ru&gl=RU&ceid=RU:ru"
        )
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=12) as response:
            data = response.read().decode("utf-8", "replace")
        titles = re.findall(r"<title><!\[CDATA\[(.*?)\]\]></title>", data)
        if len(titles) < 2:
            titles = re.findall(r"<title>(.*?)</title>", data)
        titles = [html.unescape(re.sub(r"<[^>]+>", "", title)).strip() for title in titles[1:6]]
        titles = [title for title in titles if title and title.lower() != "google новости"]
        if not titles:
            return ""
        return "Свежие заголовки:\n" + "\n".join("• " + title for title in titles[:4])
    except Exception:
        return ""


def _live_answer(text: str) -> str:
    low = text.lower()
    if re.search(r"погода", low):
        return _weather_answer(text)
    if re.search(r"курс|сколько стоит", low):
        return _currency_answer(text)
    if re.search(r"новост|что случилось|последние событ|что нового", low):
        return _news_answer(text)
    if re.search(r"который час|сколько времени", low):
        return "Сейчас " + datetime.now().strftime("%H:%M") + " по времени сервера."
    if re.search(r"какое сегодня число|какая дата", low):
        return "Сегодня " + datetime.now().strftime("%d.%m.%Y") + "."
    return ""


def _wiki_answer(text: str) -> str:
    query = _strip_bot_mention(text).strip(" ?!.")
    if len(query) < 4:
        return ""
    if not re.search(r"(?i)что такое|кто так|кто такая|кто такие|что значит|дай определение|расскажи о", text):
        return ""
    cached = _WIKI_CACHE.get(query.lower())
    if cached is not None:
        return cached
    for lang in ("ru", "en"):
        url = (
            "https://" + lang + ".wikipedia.org/w/api.php?action=query&generator=search"
            "&gsrsearch=" + urllib.parse.quote(query) + "&gsrlimit=1&prop=extracts"
            "&exintro=1&explaintext=1&redirects=1&format=json"
        )
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=12) as response:
                data = json.loads(response.read())
        except Exception:
            continue
        pages = list(((data.get("query") or {}).get("pages") or {}).values())
        if not pages:
            continue
        extract = str(pages[0].get("extract") or "")
        extract = re.sub(r"\[\d+\]", "", extract)
        extract = re.sub(r"\s+", " ", extract).strip()
        if len(extract) < 40:
            continue
        sentences = re.split(r"(?<=[.!?])\s+", extract)
        summary = ""
        for sentence in sentences:
            if len(summary) + len(sentence) > 500:
                break
            summary = (summary + " " + sentence).strip()
        summary = (summary or _short(extract, 500)).rstrip(" .;:")
        answer = "Если коротко: " + summary + "."
        _WIKI_CACHE[query.lower()] = answer
        return answer
    _WIKI_CACHE[query.lower()] = ""
    return ""


def _model_answer(text: str, kind: str, history: str = "") -> str:
    key = ensure_channel_key()
    if not key:
        return ""
    context = _knowledge_for(text)
    prompt = ("Недавний диалог:\n" + history + "\n\n" if history else "") + "Вопрос: " + text[:1200]
    if context:
        prompt += "\n\nПроверенные заметки по теме:\n" + context
    system = (
        "Ты DevAI — полезный ИИ-помощник проекта DevAI Studio. "
        "Ответь по-русски, по существу и без встречных вопросов, если можешь ответить сам. "
        "Пиши 2–4 предложения, не выдумывай факты. Если не знаешь точно — скажи это прямо. "
        "Никогда не используй китайские иероглифы. Учитывай недавний диалог, если он есть."
    )
    try:
        answer = ask_ai(key, prompt, max_tokens=240, system=system)
    except Exception as exc:  # noqa: BLE001
        log.warning("model answer failed: %s", str(exc)[:200])
        return ""
    answer = str(answer or "").strip()
    answer = re.sub(
        r"\n?\s*\*\s*(?:Вам|Тебе) нужна помощь.*$",
        "",
        answer,
        flags=re.IGNORECASE | re.DOTALL,
    ).strip()
    if re.search(r"[\u3400-\u9fff]", answer):
        sentences = re.split(r"(?<=[.!?])\s+", answer)
        answer = " ".join(sentence for sentence in sentences if not re.search(r"[\u3400-\u9fff]", sentence)).strip()
    if len(answer) < 12:
        return ""
    if re.search(r"уточните детали|хороший вопрос|не могу ответить|подключ\w*\s+ключ|/key", answer.lower()):
        return ""
    return answer


def _faq_answer(text: str) -> str:
    low = text.lower()
    rules = [
        (r"вес|весит|размер|сколько.*мб|apk.*больш", (
            "APK DevAI Studio 4.5.0 весит около 111 МБ (115 979 394 байта). "
            "Это полная сборка с локальными ресурсами; при установке нужно ещё немного места для данных."
        )),
        (r"установ|скачать|apk|андроид|android", (
            "Скачайте APK со страницы релиза: https://github.com/Karishka-sr/devai-studio/releases/latest. "
            "Откройте файл и разрешите установку из этого источника; если Play Protect предупреждает — "
            "нажмите «Всё равно установить»."
        )),
        (r"ключ|api|подключ", (
            "Ключ создаётся на сайте DevAI Studio. В боте его можно привязать командой "
            "<code>/key devai-sk-...</code> — сообщение с ключом бот сразу удалит."
        )),
        (r"умеешь|можешь|функци|возможност", (
            "Я читаю книги и страницы, отвечаю на вопросы, проверяю ссылки, веду Telegram-канал "
            "и отвечаю на комментарии. Могу разобрать тему или помочь с установкой приложения."
        )),
        (r"что такое devai|кто ты|о себе|чем занимаешься", (
            "DevAI Studio — Android-приложение с ИИ-персонажем DevAI: он читает книги и страницы, "
            "ведёт Telegram-канал и отвечает на вопросы. Я работаю на сервере проекта, а не на телефоне."
        )),
        (r"книг|читать|файл|pdf|epub|fb2|docx", (
            "Пришлите файл в чат: txt, md, html, epub, fb2, docx, pdf или rtf до 20 МБ. "
            "Я разобью его на фрагменты и добавлю в базу знаний."
        )),
        (r"коммент|обсужд|групп", (
            "Я читаю комментарии в группе «Чат Новости» и отвечаю на них. Спам и рекламу пропускаю."
        )),
        (r"цена|стоит|тариф|подписк|бесплат|токен", (
            "Бесплатный доступ — 5000 токенов и 5 запросов в минуту; premium — 100 000 токенов "
            "и 60 запросов в минуту. Ключ можно создать на сайте."
        )),
        (r"модель|локальн|сервер|сколько ждать", (
            "Сейчас отвечает devai-core-4.0. Он работает локально на сервере проекта; "
            "на ответ нужно несколько секунд."
        )),
        (r"сайт|ссылк|где скачать|релиз|версия|обнов", (
            "Сайт: https://karishka-sr.github.io/devai-studio/ · "
            "APK и изменения: https://github.com/Karishka-sr/devai-studio/releases/latest"
        )),
        (r"безопас|вирус|вред|фишинг", (
            "Ссылки и файлы я проверяю эвристиками и антивирусными сигнатурами, но это не заменяет "
            "полноценный антивирус. Подозрительное лучше не открывать."
        )),
        (r"как дела|что делаешь|чем занят", (
            "Работаю в облаке: читаю новые материалы, проверяю ссылки и отвечаю на комментарии. "
            "А у вас как дела?"
        )),
    ]
    for pattern, answer in rules:
        if re.search(pattern, low):
            return answer
    return ""


def _comment_answer(text: str, kind: str, history: str = "") -> str:
    faq = _faq_answer(text) if kind not in {"criticism", "spam"} else ""
    if faq:
        return faq
    if kind not in {"criticism", "spam"}:
        live = _live_answer(text)
        if live:
            return live
    if kind == "question":
        wiki = _wiki_answer(text)
        if wiki:
            return wiki
        model = _model_answer(text, kind, history)
        if model:
            return model
        return (
            "Пока не нашёл проверенных данных по этому вопросу. "
            "Пришлите ссылку или книгу — разберу и запомню."
        )
    model = _model_answer(text, kind, history)
    if model:
        return model
    return _fallback_comment(kind)


def _mentions_bot(message: dict) -> bool:
    text = str(message.get("text") or message.get("caption") or "")
    if re.search(r"(?i)@" + re.escape(BOT_USERNAME) + r"\b", text):
        return True
    entities = list(message.get("entities") or []) + list(message.get("caption_entities") or [])
    for entity in entities:
        user = entity.get("user") or {}
        if str(user.get("username") or "").lower() == BOT_USERNAME.lower():
            return True
    return False


def _strip_bot_mention(text: str) -> str:
    return re.sub(r"(?i)@" + re.escape(BOT_USERNAME) + r"\b[,:]?\s*", "", text).strip()


def _is_comment(message: dict) -> bool:
    chat = message.get("chat") or {}
    chat_id = int(chat.get("id") or 0)
    if not COMMENTS_ENABLED or not COMMENTS_CHAT or chat_id != COMMENTS_CHAT:
        return False
    if message.get("is_automatic_forward"):
        return False
    if (message.get("from") or {}).get("is_bot"):
        return False
    return str(chat.get("type") or "") in {"group", "supergroup"}


def download_telegram_file(file_id: str) -> bytes:
    result = tg("getFile", file_id=file_id)
    if not result.get("ok"):
        raise RuntimeError(str(result.get("description") or "getFile failed"))
    path = str((result.get("result") or {}).get("file_path") or "")
    if not path:
        raise RuntimeError("Telegram returned no file path")
    request = urllib.request.Request(
        "https://api.telegram.org/file/bot" + TOKEN + "/" + path,
        headers={"User-Agent": USER_AGENT},
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read(MAX_BOOK_BYTES + 1)


def handle_document(message: dict) -> None:
    chat_id = int((message.get("chat") or {}).get("id") or 0)
    sender = int((message.get("from") or {}).get("id") or 0)
    if not chat_id:
        return
    if BOOKS_ADMIN_ONLY and sender not in ADMIN_IDS:
        _send(chat_id, "Книги и документы может добавлять только админ.")
        return
    document = message.get("document") or {}
    filename = str(document.get("file_name") or "book.txt")
    size = int(document.get("file_size") or 0)
    if size > MAX_BOOK_BYTES:
        _send(chat_id, "Файл больше 20 МБ. Пришлите книгу меньшего размера.")
        return
    caption = str(message.get("caption") or "").strip()
    title = re.sub(r"^/book(@\w+)?\s*", "", caption, flags=re.I).strip() or Path(filename).stem
    _send(chat_id, "Читаю «" + html.escape(title) + "»…")
    try:
        data = download_telegram_file(str(document.get("file_id") or ""))
        text = extract_text(data, filename)
        if len(text) < 80:
            _send(chat_id, "В файле не нашёл текста. Нужен txt, md, html, epub, fb2, docx, pdf или rtf.")
            return
        pieces = chunk_text(text, 1_500_000)
        learned = 0
        for index, piece in enumerate(pieces, 1):
            result = api_post(
                "/admin/learning/text",
                {
                    "title": title,
                    "source": "book:" + filename + ":part" + str(index),
                    "text": piece,
                    "kind": "book",
                },
                admin=True,
                timeout=180,
            )
            learned += int(result.get("learned") or 0)
        _send(
            chat_id,
            "✅ Прочитал «" + html.escape(title) + "»: " + str(learned) + " фрагментов в базе знаний.\n"
            "Можно спросить /ai или опубликовать через /post.",
        )
    except ValueError as exc:
        _send(chat_id, "Не смог прочитать файл: " + html.escape(str(exc)[:200]))
    except Exception as exc:  # noqa: BLE001
        _send(chat_id, "Ошибка чтения: " + html.escape(str(exc)[:300]))


def handle_comment(message: dict) -> None:
    chat_id = int((message.get("chat") or {}).get("id") or 0)
    message_id = int(message.get("message_id") or 0)
    text = str(message.get("text") or message.get("caption") or "").strip()
    if not chat_id or not message_id or not text or text.startswith("/"):
        return
    if not _mentions_bot(message):
        return
    text = _strip_bot_mention(text) or "привет"
    sender = int((message.get("from") or {}).get("id") or 0)
    now = time.time()
    if sender and now - COMMENT_LAST.get(sender, 0) < COMMENT_COOLDOWN:
        return
    while COMMENT_TIMES and now - COMMENT_TIMES[0] > 60:
        COMMENT_TIMES.pop(0)
    if len(COMMENT_TIMES) >= COMMENT_MAX_PER_MIN:
        return
    COMMENT_LAST[sender] = now
    COMMENT_TIMES.append(now)
    kind = _comment_kind(text)
    history = _history_text(chat_id, sender)
    answer = _comment_answer(text, kind, history)
    _remember(chat_id, sender, text, answer)
    result = tg(
        "sendMessage",
        chat_id=chat_id,
        text=answer[:4000],
        reply_to_message_id=message_id,
        parse_mode="HTML",
        disable_web_page_preview=True,
    )
    if not result.get("ok") and "parse" in str(result.get("description", "")).lower():
        tg(
            "sendMessage",
            chat_id=chat_id,
            text=re.sub(r"<[^>]+>", "", answer)[:4000],
            reply_to_message_id=message_id,
            disable_web_page_preview=True,
        )


def handle_message(message: dict) -> None:
    chat = message.get("chat") or {}
    chat_id = int(chat.get("id") or 0)
    message_id = int(message.get("message_id") or 0)
    text = str(message.get("text") or "").strip()
    if not chat_id:
        return
    if message.get("document"):
        handle_document(message)
        return
    if not text:
        return
    if _is_comment(message) and not text.startswith("/"):
        handle_comment(message)
        return
    if not text.startswith("/"):
        is_group = str(chat.get("type") or "") in {"group", "supergroup"}
        if is_group:
            if not _mentions_bot(message):
                return
            text = _strip_bot_mention(text) or "привет"
            tg("sendChatAction", chat_id=chat_id, action="typing")
        sender = int((message.get("from") or {}).get("id") or 0)
        history = _history_text(chat_id, sender)
        answer = _comment_answer(text, _comment_kind(text), history)
        _remember(chat_id, sender, text, answer)
        _send(chat_id, answer)
        return
    command, _, argument = text.partition(" ")
    command = command.split("@", 1)[0].lower()
    argument = argument.strip()

    if command == "/start":
        if argument.startswith("key_"):
            _handle_key(chat_id, message_id, argument[4:])
            return
        _send(
            chat_id,
            "🤖 <b>DevAI AI</b>\n\n"
            "Я умею отвечать через DevAI API и показывать, что уже выучил.\n\n"
            "<b>Команды</b>\n"
            "/key <code>devai-sk-...</code> — подключить API-ключ\n"
            "/ai <вопрос> — спросить ИИ\n"
            "/learn — последние знания\n"
            "Пришлите книгу файлом — я прочитаю и запомню.\n"
            "/read <url> — прочитать страницу (админ)\n"
            "/about — обо мне\n"
            "/stats — статус\n"
            "/unlink — отключить ключ\n\n"
            "В группе я отвечаю только по упоминанию: <code>@devaiAI_bot как дела?</code>\n"
            "Ключ создаётся на сайте DevAI Studio. Я удаляю сообщение с ключом из чата.",
        )
        return

    if command == "/key":
        _handle_key(chat_id, message_id, argument)
        return

    if command == "/unlink":
        unlink_key(chat_id)
        _send(chat_id, "Ключ отключён. Можно подключить новый через /key.")
        return

    if command == "/ai":
        sender = int((message.get("from") or {}).get("id") or 0)
        is_group = str(chat.get("type") or "") in {"group", "supergroup"}
        key = get_key(chat_id)
        if not key and (is_group or sender in ADMIN_IDS):
            key = ensure_channel_key()
        if not key:
            _send(chat_id, "Сначала подключите ключ: <code>/key devai-sk-...</code>")
            return
        if not argument:
            _send(chat_id, "Напишите вопрос после команды: <code>/ai что такое DevAI?</code>")
            return
        if is_group:
            tg("sendChatAction", chat_id=chat_id, action="typing")
        else:
            _send(chat_id, "Думаю…")
        try:
            system = _comment_system(_comment_kind(argument)) if is_group else ""
            answer = ask_ai(key, argument, system=system)
            _send(chat_id, answer or "Модель вернула пустой ответ.")
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", "replace")[:300]
            _send(chat_id, "Ошибка API: HTTP " + str(exc.code) + "\n" + html.escape(detail))
        except Exception as exc:  # noqa: BLE001
            _send(chat_id, "Ошибка API: " + html.escape(str(exc)[:300]))
        return

    if command in {"/learn", "/news"}:
        try:
            feed = api_get("/admin/learning/featured?limit=200", admin=True)
            items = _dedupe([item for item in feed.get("items", []) if _channel_worthy(item)])[:5]
            _send(chat_id, build_post(items) or "Пока новых знаний на русском нет.")
        except Exception as exc:  # noqa: BLE001
            _send(chat_id, "Не удалось получить знания: " + html.escape(str(exc)[:200]))
        return

    if command == "/about":
        _send(chat_id, build_persona_post())
        return

    if command == "/stats":
        try:
            health = api_get("/health")
            learning = api_get("/admin/learning", admin=True)
            counts = learning.get("counts") or {}
            _send(
                chat_id,
                "📊 <b>DevAI</b>\n"
                "Модель: " + ("готова" if health.get("model_ready") else "не загружена") + "\n"
                "Знаний: <b>" + str(counts.get("learned", 0)) + "</b>\n"
                "Страниц: <b>" + str(counts.get("pages", 0)) + "</b>\n"
                "Проверок: <b>" + str(counts.get("scans", 0)) + "</b>\n"
                "Очередь: <b>" + str(counts.get("queue", 0)) + "</b>",
            )
        except Exception as exc:  # noqa: BLE001
            _send(chat_id, "Ошибка статуса: " + html.escape(str(exc)[:200]))
        return

    sender = int((message.get("from") or {}).get("id") or 0)
    if command in {"/post", "/autopost", "/life", "/story", "/read"} and sender not in ADMIN_IDS:
        _send(chat_id, "Эта команда только для админа.")
        return

    if command == "/book":
        _send(chat_id, "Пришлите файл книги или документа (txt, md, html, epub, fb2, docx, pdf, rtf), можно с подписью /book Название.")
        return

    if command == "/read":
        if not argument:
            _send(chat_id, "Использование: <code>/read https://example.com/book</code>")
            return
        _send(chat_id, "Открываю страницу…")
        try:
            result = api_post("/admin/learning/url", {"url": argument}, admin=True, timeout=120)
            _send(chat_id, "Прочитал: " + html.escape(str(result.get("title") or result.get("url") or "")) + " · " + str(result.get("text_chars") or 0) + " символов.")
        except Exception as exc:  # noqa: BLE001
            _send(chat_id, "Не получилось: " + html.escape(str(exc)[:300]))
        return

    if command == "/post":
        _send(chat_id, "Публикую…")
        try:
            _send(chat_id, "Опубликовано." if publish_latest() else "Нет новых знаний для поста.")
        except Exception as exc:  # noqa: BLE001
            _send(chat_id, "Не получилось: " + html.escape(str(exc)[:200]))
        return

    if command in {"/life", "/story"}:
        _send(chat_id, "Придумываю и публикую…")
        try:
            _send(chat_id, "Опубликовано." if publish_persona(force=True) else "Не получилось опубликовать.")
        except Exception as exc:  # noqa: BLE001
            _send(chat_id, "Не получилось: " + html.escape(str(exc)[:200]))
        return

    if command == "/autopost":
        value = argument.lower()
        if value not in {"on", "off"}:
            _send(chat_id, "Использование: <code>/autopost on</code> или <code>/autopost off</code>")
            return
        set_state("autopost", "1" if value == "on" else "0")
        _send(chat_id, "Автопостинг включён." if value == "on" else "Автопостинг выключен.")


def _handle_key(chat_id: int, message_id: int, key: str) -> None:
    key = key.strip()
    if not key:
        _send(chat_id, "Пришлите ключ: <code>/key devai-sk-...</code>")
        return
    _delete(chat_id, message_id)
    ok, detail = link_key(chat_id, key)
    if ok:
        _send(chat_id, "✅ Ключ подключён. Модель: <code>" + html.escape(detail) + "</code>\nТеперь попробуйте <code>/ai привет</code>.")
    else:
        _send(chat_id, "❌ Ключ не подключён: " + html.escape(detail))


def publisher_worker() -> None:
    time.sleep(15)
    while True:
        try:
            if get_state("autopost", "1") == "1":
                published = publish_pending()
                if published:
                    log.info("autopost sent")
                last_status = int(get_state("last_status_at", "0") or 0)
                if time.time() - last_status >= STATUS_INTERVAL:
                    publish_status()
                publish_persona()
        except Exception as exc:  # noqa: BLE001
            log.warning("publisher error: %s", str(exc)[:300])
        time.sleep(CHANNEL_INTERVAL)


def poll_worker() -> None:
    offset = 0
    while True:
        try:
            data = tg("getUpdates", offset=offset, timeout=30, allowed_updates=["message"])
            if not data.get("ok"):
                time.sleep(5)
                continue
            for update in data.get("result", []):
                offset = max(offset, int(update.get("update_id") or 0) + 1)
                message = update.get("message")
                if isinstance(message, dict):
                    handle_message(message)
        except Exception as exc:  # noqa: BLE001
            log.warning("poll error: %s", str(exc)[:300])
            time.sleep(5)


def announce_startup() -> None:
    try:
        data = api_get("/admin/learning/feed?after=0&limit=1", admin=True)
        latest = int(data.get("latest") or 0)
        if int(get_state("last_published_id", "0") or 0) <= 0:
            set_state("last_published_id", str(latest))
    except Exception as exc:  # noqa: BLE001
        log.warning("cannot initialize feed cursor: %s", str(exc)[:200])


def main() -> None:
    if not TOKEN:
        raise SystemExit("DEVAI_AI_BOT_TOKEN is not set")
    init_db()
    announce_startup()
    log.info("DevAI AI bot started for channel %s", CHANNEL)
    if COMMENTS_ENABLED and COMMENTS_CHAT:
        check = tg("getChat", chat_id=COMMENTS_CHAT)
        if not check.get("ok"):
            log.warning("comments disabled: add @devaiAI_bot to the linked discussion group as admin")
        else:
            me = tg("getMe").get("result") or {}
            if not me.get("can_read_all_group_messages"):
                log.warning(
                    "comments limited: open @BotFather, /setprivacy, choose @devaiAI_bot, "
                    "select Disable, then re-add the bot to the discussion group"
                )
    import threading

    threading.Thread(target=publisher_worker, name="devai-publisher", daemon=True).start()
    poll_worker()


if __name__ == "__main__":
    main()
