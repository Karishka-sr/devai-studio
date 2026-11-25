#!/usr/bin/env python3
"""DevAI Core v0.1 API server.

OpenAI-compatible /v1/chat/completions backed by a local GGUF model.
API keys are stored hashed in SQLite; the admin token is local-only.
"""
from __future__ import annotations

import hashlib
import hmac
import json
import os
import re
import secrets
import sqlite3
import threading
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

SERVER_VERSION = "0.3.0"
MODEL_ID = "devai-core-0.1"
ROOT = Path(__file__).resolve().parent
DATA = Path(os.environ.get("DEVAI_DATA_DIR", ROOT / "data"))
DATA.mkdir(parents=True, exist_ok=True)
DB_PATH = DATA / "devai.db"
ADMIN_FILE = DATA / "admin_token"
DEFAULT_MODEL = Path(
    os.environ.get(
        "DEVAI_MODEL_PATH",
        str(ROOT / "models" / "qwen2.5-0.5b-instruct-q4_k_m.gguf"),
    )
)
HOST = os.environ.get("DEVAI_HOST", "0.0.0.0")
PORT = int(os.environ.get("DEVAI_PORT", "12001"))
MAX_BODY = 2 * 1024 * 1024
MAX_TOKENS = 1024
FREE_TOKEN_LIMIT = max(1000, min(int(os.environ.get("DEVAI_FREE_TOKENS", "5000")), 10_000_000))
PREMIUM_TOKEN_LIMIT = max(FREE_TOKEN_LIMIT, min(int(os.environ.get("DEVAI_PREMIUM_TOKENS", "100000")), 100_000_000))
FREE_RPM = max(1, min(int(os.environ.get("DEVAI_FREE_RPM", "5")), 600))
PREMIUM_RPM = max(FREE_RPM, min(int(os.environ.get("DEVAI_PREMIUM_RPM", "60")), 600))
QUOTA_PERIOD = max(1, int(os.environ.get("DEVAI_QUOTA_DAYS", "30"))) * 86400
REGISTER_PER_IP_DAY = 3
REGISTER_GLOBAL_DAY = 300
CHALLENGE_TTL = 300
CHALLENGE_DIFFICULTY = 4
CHALLENGE_MIN_AGE = 1.2
CHALLENGE_PER_IP_ACTIVE = 8
PROMO_PLANS = {
    "week": {"days": 7, "title": "Неделя"},
    "month": {"days": 30, "title": "Месяц"},
    "year": {"days": 365, "title": "Год"},
    "lifetime": {"days": 0, "title": "Навсегда"},
}
PROMO_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
PROMO_ATTEMPTS: dict[str, list[float]] = {}
PROMO_ATTEMPT_LOCK = threading.Lock()
CHALLENGES: dict[str, tuple[float, str]] = {}
CHALLENGE_LOCK = threading.Lock()

SYSTEM_PROMPT = """Ты — DevAI Core v0.1, собственный ИИ DevAI Studio.
Ты помогаешь разработчику: код, архитектура, ошибки, тесты, интерфейс и тексты.
Правила:
- отвечай по-русски, если пользователь пишет по-русски;
- давай конкретные шаги и готовый код, если он уместен;
- не выдумывай факты о проекте;
- если данных не хватает, задай один точный вопрос;
- не притворяйся, что выполнил действие, которого не было.
Ты не OpenAI, не Google и не другая внешняя модель: ты DevAI Core.
"""

DB_LOCK = threading.Lock()
GEN_LOCK = threading.Lock()
MODEL = None
MODEL_ERROR = None
MODEL_LOAD_LOCK = threading.Lock()


def db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, timeout=30)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db() -> None:
    with db() as conn:
        conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS keys (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                prefix TEXT NOT NULL,
                hash TEXT NOT NULL UNIQUE,
                created REAL NOT NULL,
                last_used REAL,
                requests INTEGER NOT NULL DEFAULT 0,
                rpm INTEGER NOT NULL DEFAULT 5,
                revoked INTEGER NOT NULL DEFAULT 0,
                tier TEXT NOT NULL DEFAULT 'free',
                token_limit INTEGER NOT NULL DEFAULT 0,
                tokens_used INTEGER NOT NULL DEFAULT 0,
                period_start REAL NOT NULL DEFAULT 0,
                premium_until REAL NOT NULL DEFAULT 0
            );
            CREATE TABLE IF NOT EXISTS usage (
                key_id TEXT NOT NULL,
                ts REAL NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_usage_key_ts ON usage(key_id, ts);
            CREATE TABLE IF NOT EXISTS signups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ip_hash TEXT NOT NULL,
                key_id TEXT NOT NULL,
                created REAL NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_signups_ip_ts ON signups(ip_hash, created);
            CREATE TABLE IF NOT EXISTS promo_codes (
                code TEXT PRIMARY KEY,
                plan TEXT NOT NULL,
                days INTEGER NOT NULL,
                max_uses INTEGER NOT NULL DEFAULT 1,
                used INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                note TEXT NOT NULL DEFAULT '',
                created REAL NOT NULL,
                expires REAL
            );
            CREATE TABLE IF NOT EXISTS promo_redemptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL,
                plan TEXT NOT NULL,
                device_hash TEXT NOT NULL,
                ip_hash TEXT NOT NULL,
                created REAL NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_promo_redemptions_code_device
                ON promo_redemptions(code, device_hash);
            """
        )
        columns = {row["name"] for row in conn.execute("PRAGMA table_info(keys)")}
        for name, ddl in (
            ("tier", "TEXT NOT NULL DEFAULT 'free'"),
            ("token_limit", "INTEGER NOT NULL DEFAULT 0"),
            ("tokens_used", "INTEGER NOT NULL DEFAULT 0"),
            ("period_start", "REAL NOT NULL DEFAULT 0"),
            ("premium_until", "REAL NOT NULL DEFAULT 0"),
        ):
            if name not in columns:
                conn.execute(f"ALTER TABLE keys ADD COLUMN {name} {ddl}")
        conn.execute(
            "UPDATE keys SET token_limit = ? WHERE token_limit IS NULL OR (token_limit <= 0 AND tier <> 'admin')",
            (FREE_TOKEN_LIMIT,),
        )
        conn.execute(
            "UPDATE keys SET period_start = ? WHERE period_start IS NULL OR period_start <= 0",
            (time.time(),),
        )


def admin_token() -> str:
    env = os.environ.get("DEVAI_ADMIN_TOKEN", "").strip()
    if env:
        return env
    if ADMIN_FILE.exists():
        return ADMIN_FILE.read_text().strip()
    token = "devai-admin-" + secrets.token_urlsafe(32)
    ADMIN_FILE.write_text(token)
    os.chmod(ADMIN_FILE, 0o600)
    return token


ADMIN_TOKEN = admin_token()


def hash_key(raw: str) -> str:
    return hashlib.sha256(raw.encode()).hexdigest()


def effective_tier(row, now: float | None = None) -> str:
    now = time.time() if now is None else now
    tier = str(row["tier"] or "free")
    if tier == "admin":
        return "admin"
    if tier == "premium" and float(row["premium_until"] or 0) > now:
        return "premium"
    return "free"


def tier_limits(row, tier: str) -> tuple[int, int]:
    if tier == "admin":
        return 0, max(1, min(int(row["rpm"] or 60), 600))
    if tier == "premium":
        return PREMIUM_TOKEN_LIMIT, PREMIUM_RPM
    return FREE_TOKEN_LIMIT, FREE_RPM


def estimate_tokens(text: str) -> int:
    return max(1, len(text or "") // 4)


def estimate_message_tokens(messages: list[dict]) -> int:
    return sum(estimate_tokens(str(item.get("content", ""))) + 4 for item in messages)


def charge_tokens(key_id: str, tokens: int) -> None:
    tokens = max(0, int(tokens))
    if tokens <= 0:
        return
    with DB_LOCK, db() as conn:
        conn.execute("UPDATE keys SET tokens_used = tokens_used + ? WHERE id = ?", (tokens, key_id))


def new_key() -> tuple[str, str]:
    raw = "devai-sk-" + secrets.token_urlsafe(32)
    return raw, hash_key(raw)


def ip_digest(ip: str) -> str:
    return hashlib.sha256((ADMIN_TOKEN + "|" + ip).encode()).hexdigest()


def issue_challenge(ip: str) -> str | None:
    now = time.time()
    challenge = secrets.token_hex(16)
    digest = ip_digest(ip)
    with CHALLENGE_LOCK:
        for key, (ts, _) in list(CHALLENGES.items()):
            if ts < now - CHALLENGE_TTL:
                del CHALLENGES[key]
        active = sum(1 for _, item_digest in CHALLENGES.values() if item_digest == digest)
        if active >= CHALLENGE_PER_IP_ACTIVE:
            return None
        CHALLENGES[challenge] = (now, digest)
    return challenge


def verify_challenge(challenge: str, solution: str, ip: str) -> bool:
    with CHALLENGE_LOCK:
        item = CHALLENGES.pop(challenge, None)
    if item is None:
        return False
    created, digest = item
    age = time.time() - created
    if age < CHALLENGE_MIN_AGE or age > CHALLENGE_TTL or digest != ip_digest(ip):
        return False
    text = challenge + ":" + solution
    return hashlib.sha256(text.encode()).hexdigest().startswith("0" * CHALLENGE_DIFFICULTY)


def model_ready() -> bool:
    return MODEL is not None


def get_model():
    global MODEL, MODEL_ERROR
    if MODEL is not None:
        return MODEL
    with MODEL_LOAD_LOCK:
        if MODEL is not None:
            return MODEL
        try:
            from llama_cpp import Llama

            if not DEFAULT_MODEL.exists():
                raise FileNotFoundError(f"model not found: {DEFAULT_MODEL}")
            MODEL = Llama(
                model_path=str(DEFAULT_MODEL),
                n_ctx=2048,
                n_threads=max(1, min(4, os.cpu_count() or 1)),
                verbose=False,
            )
            MODEL_ERROR = None
        except Exception as exc:  # noqa: BLE001
            MODEL_ERROR = str(exc)
            raise
        return MODEL


def clean_messages(raw_messages) -> list[dict]:
    if not isinstance(raw_messages, list) or not raw_messages:
        raise ValueError("messages must be a non-empty array")
    out: list[dict] = []
    for item in raw_messages[-24:]:
        if not isinstance(item, dict):
            continue
        role = str(item.get("role", "")).strip()
        if role not in {"system", "user", "assistant"}:
            continue
        content = item.get("content", "")
        if isinstance(content, list):
            content = " ".join(
                str(part.get("text", ""))
                for part in content
                if isinstance(part, dict) and part.get("type") == "text"
            )
        content = str(content or "").strip()
        if content:
            out.append({"role": role, "content": content})
    if not out:
        raise ValueError("messages have no text content")
    if out[0]["role"] == "system":
        out[0]["content"] = SYSTEM_PROMPT + "\n\n" + out[0]["content"]
    else:
        out.insert(0, {"role": "system", "content": SYSTEM_PROMPT})
    # Keep the context small enough for 2048 tokens.
    while len(out) > 2 and sum(len(m["content"]) for m in out) > 7000:
        out.pop(1)
    return out


def normalize_promo(raw: str) -> str:
    return re.sub(r"[^A-Z0-9]+", "-", str(raw or "").strip().upper()).strip("-")


def new_promo_code() -> str:
    body = "".join(secrets.choice(PROMO_ALPHABET) for _ in range(8))
    return "DVS-" + body[:4] + "-" + body[4:]


def create_promo_codes(
    plan: str,
    count: int,
    max_uses: int = 1,
    days: int | None = None,
    expires_days: int = 30,
    note: str = "",
) -> list[str]:
    plan = str(plan or "").strip().lower()
    if plan not in PROMO_PLANS:
        raise ValueError("unknown plan")
    count = max(1, min(int(count), 100))
    max_uses = max(1, min(int(max_uses), 1000))
    plan_days = PROMO_PLANS[plan]["days"]
    days = plan_days if days is None else max(0, min(int(days), 3650))
    expires_days = max(0, min(int(expires_days), 3650))
    expires = time.time() + expires_days * 86400 if expires_days else None
    codes: list[str] = []
    with DB_LOCK, db() as conn:
        for _ in range(count * 5):
            if len(codes) >= count:
                break
            code = new_promo_code()
            try:
                conn.execute(
                    "INSERT INTO promo_codes(code, plan, days, max_uses, note, created, expires) "
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (code, plan, days, max_uses, note[:200], time.time(), expires),
                )
                codes.append(code)
            except sqlite3.IntegrityError:
                continue
    if len(codes) < count:
        raise RuntimeError("could not allocate enough unique codes")
    return codes


def promo_rate_ok(ip_hash: str) -> bool:
    now = time.time()
    with PROMO_ATTEMPT_LOCK:
        bucket = [item for item in PROMO_ATTEMPTS.get(ip_hash, []) if item > now - 60]
        if len(bucket) >= 20:
            PROMO_ATTEMPTS[ip_hash] = bucket
            return False
        bucket.append(now)
        PROMO_ATTEMPTS[ip_hash] = bucket
    return True


def activate_promo(code: str, device_hash: str, ip_hash: str) -> tuple[int, dict]:
    code = normalize_promo(code)
    if not code or len(code) > 40:
        return 400, {"error": {"message": "Введите промокод.", "type": "invalid_request_error"}}
    if not device_hash:
        return 400, {"error": {"message": "Не удалось определить устройство.", "type": "invalid_request_error"}}
    now = time.time()
    with DB_LOCK, db() as conn:
        row = conn.execute("SELECT * FROM promo_codes WHERE code = ?", (code,)).fetchone()
        if row is None:
            return 404, {"error": {"message": "Промокод не найден.", "type": "not_found"}}
        if not row["active"]:
            return 410, {"error": {"message": "Промокод отключён.", "type": "gone"}}
        if row["expires"] and row["expires"] < now:
            return 410, {"error": {"message": "Срок действия промокода истёк.", "type": "gone"}}
        existing = conn.execute(
            "SELECT plan FROM promo_redemptions WHERE code = ? AND device_hash = ?",
            (code, device_hash),
        ).fetchone()
        if existing:
            plan = existing["plan"]
            return 200, {
                "ok": True,
                "code": code,
                "plan": plan,
                "days": PROMO_PLANS[plan]["days"],
                "title": PROMO_PLANS[plan]["title"],
                "already_redeemed": True,
            }
        if row["used"] >= row["max_uses"]:
            return 409, {"error": {"message": "Промокод уже использован.", "type": "conflict"}}
        conn.execute("UPDATE promo_codes SET used = used + 1 WHERE code = ?", (code,))
        conn.execute(
            "INSERT INTO promo_redemptions(code, plan, device_hash, ip_hash, created) VALUES (?, ?, ?, ?, ?)",
            (code, row["plan"], device_hash, ip_hash, now),
        )
    plan = row["plan"]
    return 200, {
        "ok": True,
        "code": code,
        "plan": plan,
        "days": row["days"],
        "title": PROMO_PLANS[plan]["title"],
        "already_redeemed": False,
    }



class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    server_version = "DevAI-Core/" + SERVER_VERSION

    def log_message(self, fmt: str, *args) -> None:
        print("[%s] %s" % (self.log_date_time_string(), fmt % args), flush=True)

    def _cors(self) -> None:
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Admin-Token")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")

    def _send_json(self, code: int, payload: dict, extra: dict | None = None) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self._cors()
        for k, v in (extra or {}).items():
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(body)

    def _error(self, code: int, message: str, kind: str = "invalid_request_error") -> None:
        self._send_json(
            code,
            {"error": {"message": message, "type": kind, "code": str(code)}},
        )

    def _read_json(self) -> dict:
        length = int(self.headers.get("Content-Length", "0") or "0")
        if length <= 0:
            return {}
        if length > MAX_BODY:
            raise ValueError("too large body")
        raw = self.rfile.read(length)
        data = json.loads(raw.decode("utf-8"))
        if not isinstance(data, dict):
            raise ValueError("body must be an object")
        return data

    def _bearer(self) -> str:
        auth = self.headers.get("Authorization", "")
        if auth.lower().startswith("bearer "):
            return auth[7:].strip()
        return ""

    def _client_ip(self) -> str:
        forwarded = self.headers.get("X-Forwarded-For", "")
        if forwarded:
            return forwarded.split(",")[0].strip()
        return self.client_address[0]

    def _is_admin(self) -> bool:
        token = self.headers.get("X-Admin-Token", "").strip() or self._bearer()
        return bool(token) and hmac.compare_digest(token, ADMIN_TOKEN)

    def _key_row(self, *, record: bool = True):
        raw = self._bearer()
        if not raw.startswith("devai-sk-"):
            self._error(401, "Missing or invalid API key.", "authentication_error")
            return None
        digest = hash_key(raw)
        now = time.time()
        with DB_LOCK, db() as conn:
            row = conn.execute(
                "SELECT * FROM keys WHERE hash = ? AND revoked = 0", (digest,)
            ).fetchone()
            if row is None:
                self._error(401, "API key not found or revoked.", "authentication_error")
                return None

            period_start = float(row["period_start"] or 0)
            if period_start <= 0 or now - period_start >= QUOTA_PERIOD:
                conn.execute(
                    "UPDATE keys SET tokens_used = 0, period_start = ? WHERE id = ?",
                    (now, row["id"]),
                )
                used = 0
            else:
                used = int(row["tokens_used"] or 0)

            tier = effective_tier(row, now)
            limit, rpm = tier_limits(row, tier)

            if record:
                conn.execute("DELETE FROM usage WHERE ts < ?", (now - 3600,))
                recent = conn.execute(
                    "SELECT COUNT(*) FROM usage WHERE key_id = ? AND ts > ?",
                    (row["id"], now - 60),
                ).fetchone()[0]
                if recent >= rpm:
                    self._error(429, "Rate limit exceeded. Try again in a minute.", "rate_limit_error")
                    return None
                if limit > 0 and used >= limit:
                    self._send_json(
                        429,
                        {"error": {"message": "Monthly token limit reached. Upgrade with a Premium promo code.", "type": "rate_limit_error", "code": "429"}},
                        extra={"X-DevAI-Tier": tier, "X-DevAI-Tokens-Limit": str(limit), "X-DevAI-Tokens-Used": str(used), "X-DevAI-Tokens-Remaining": "0"},
                    )
                    return None
                conn.execute("INSERT INTO usage(key_id, ts) VALUES (?, ?)", (row["id"], now))
                conn.execute(
                    "UPDATE keys SET last_used = ?, requests = requests + 1 WHERE id = ?",
                    (now, row["id"]),
                )

            data = dict(row)
            data["_tier"] = tier
            data["_token_limit"] = limit
            data["_tokens_used"] = used
            data["_rpm"] = rpm
            return data

    def _quota_headers(self, key: dict, extra_tokens: int = 0) -> dict[str, str]:
        limit = int(key["_token_limit"])
        used = int(key["_tokens_used"]) + max(0, int(extra_tokens))
        headers = {"X-DevAI-Tier": str(key["_tier"]), "X-DevAI-Tokens-Used": str(used)}
        if limit > 0:
            headers["X-DevAI-Tokens-Limit"] = str(limit)
            headers["X-DevAI-Tokens-Remaining"] = str(max(0, limit - used))
        else:
            headers["X-DevAI-Tokens-Limit"] = "unlimited"
            headers["X-DevAI-Tokens-Remaining"] = "unlimited"
        return headers

    def do_OPTIONS(self) -> None:  # noqa: N802
        self.send_response(204)
        self._cors()
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self) -> None:  # noqa: N802
        path = urllib.parse.urlparse(self.path).path
        if path == "/health":
            self._send_json(
                200,
                {
                    "status": "ok" if model_ready() else "degraded",
                    "version": SERVER_VERSION,
                    "model": MODEL_ID,
                    "model_ready": model_ready(),
                    "model_error": MODEL_ERROR,
                    "signup_enabled": True,
                    "free_tokens": FREE_TOKEN_LIMIT,
                    "premium_tokens": PREMIUM_TOKEN_LIMIT,
                    "free_rpm": FREE_RPM,
                    "premium_rpm": PREMIUM_RPM,
                },
            )
            return
        if path == "/":
            self._send_json(
                200,
                {
                    "name": "DevAI Core API",
                    "version": SERVER_VERSION,
                    "model": MODEL_ID,
                    "endpoints": [
                        "GET /health",
                        "GET /v1/models",
                        "POST /v1/chat/completions",
                        "POST /v1/keys/challenge",
                        "POST /v1/keys/register",
                        "GET /v1/keys/me",
                        "POST /v1/keys/upgrade",
                        "POST /v1/promo/activate",
                        "POST /admin/promo",
                        "GET /admin/promo",
                    ],
                },
            )
            return
        if path == "/v1/models":
            if self._key_row() is None:
                return
            self._send_json(
                200,
                {
                    "object": "list",
                    "data": [
                        {
                            "id": MODEL_ID,
                            "object": "model",
                            "created": 0,
                            "owned_by": "devai-studio",
                        }
                    ],
                },
            )
            return
        if path == "/v1/keys/me":
            key = self._key_row(record=False)
            if key is None:
                return
            limit = int(key["_token_limit"])
            used = int(key["_tokens_used"])
            self._send_json(
                200,
                {
                    "id": key["id"],
                    "name": key["name"],
                    "tier": key["_tier"],
                    "token_limit": limit,
                    "tokens_used": used,
                    "tokens_remaining": max(0, limit - used) if limit > 0 else -1,
                    "period_start": key["period_start"],
                    "premium_until": key["premium_until"],
                    "rpm": key["_rpm"],
                    "model": MODEL_ID,
                },
                extra=self._quota_headers(key),
            )
            return
        if path == "/admin/keys":
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            with db() as conn:
                rows = conn.execute(
                    "SELECT id, name, prefix, created, last_used, requests, rpm, revoked, "
                    "tier, token_limit, tokens_used, period_start, premium_until "
                    "FROM keys ORDER BY created DESC"
                ).fetchall()
            self._send_json(200, {"keys": [dict(r) for r in rows]})
            return
        if path == "/admin/promo":
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            query = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
            plan = (query.get("plan") or [""])[0].strip().lower()
            with db() as conn:
                if plan:
                    rows = conn.execute(
                        "SELECT code, plan, days, max_uses, used, active, note, created, expires "
                        "FROM promo_codes WHERE plan = ? ORDER BY created DESC LIMIT 500",
                        (plan,),
                    ).fetchall()
                else:
                    rows = conn.execute(
                        "SELECT code, plan, days, max_uses, used, active, note, created, expires "
                        "FROM promo_codes ORDER BY created DESC LIMIT 500"
                    ).fetchall()
            self._send_json(200, {"codes": [dict(row) for row in rows]})
            return
        if path == "/admin/stats":
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            with db() as conn:
                keys = conn.execute("SELECT COUNT(*) FROM keys WHERE revoked = 0").fetchone()[0]
                requests = conn.execute("SELECT COALESCE(SUM(requests), 0) FROM keys").fetchone()[0]
            self._send_json(
                200,
                {"active_keys": keys, "total_requests": requests, "model_ready": model_ready()},
            )
            return
        self._error(404, "Not found.", "not_found")

    def do_DELETE(self) -> None:  # noqa: N802
        path = urllib.parse.urlparse(self.path).path
        parts = path.strip("/").split("/")
        if len(parts) == 3 and parts[0] == "admin" and parts[1] == "promo":
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            code = normalize_promo(parts[2])
            with db() as conn:
                cur = conn.execute("UPDATE promo_codes SET active = 0 WHERE code = ?", (code,))
            if cur.rowcount == 0:
                self._error(404, "Promo code not found.", "not_found")
                return
            self._send_json(200, {"code": code, "active": False})
            return
        if len(parts) == 3 and parts[0] == "admin" and parts[1] == "keys":
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            key_id = parts[2]
            with db() as conn:
                cur = conn.execute("UPDATE keys SET revoked = 1 WHERE id = ?", (key_id,))
            if cur.rowcount == 0:
                self._error(404, "Key not found.", "not_found")
                return
            self._send_json(200, {"id": key_id, "revoked": True})
            return
        self._error(404, "Not found.", "not_found")



    def do_POST(self) -> None:  # noqa: N802
        path = urllib.parse.urlparse(self.path).path
        try:
            body = self._read_json()
        except Exception as exc:  # noqa: BLE001
            self._error(400, f"Invalid JSON: {exc}")
            return

        if path == "/v1/chat/completions":
            key = self._key_row()
            if key is None:
                return
            self._chat(body, key)
            return

        if path == "/v1/keys/upgrade":
            self._upgrade_key(body)
            return

        if path == "/v1/keys/challenge":
            challenge = issue_challenge(self._client_ip())
            if challenge is None:
                self._error(429, "Too many open challenges. Try again in a few minutes.", "rate_limit_error")
                return
            self._send_json(
                200,
                {
                    "challenge": challenge,
                    "difficulty": CHALLENGE_DIFFICULTY,
                    "min_delay": CHALLENGE_MIN_AGE,
                    "expires_in": CHALLENGE_TTL,
                },
            )
            return

        if path == "/v1/keys/register":
            ip = self._client_ip()
            if str(body.get("website", "")).strip():
                self._error(400, "Bot check failed.")
                return
            challenge = str(body.get("challenge", "")).strip()
            solution = str(body.get("solution", "")).strip()
            if len(challenge) != 32 or not solution.isdigit() or len(solution) > 12:
                self._error(400, "Invalid challenge solution.")
                return
            if not verify_challenge(challenge, solution, ip):
                self._error(400, "Challenge failed or expired. Reload the page and try again.")
                return
            name = str(body.get("name", "devai-user")).strip()[:80] or "devai-user"
            now = time.time()
            digest = ip_digest(ip)
            with DB_LOCK, db() as conn:
                conn.execute("DELETE FROM signups WHERE created < ?", (now - 30 * 86400,))
                per_ip = conn.execute(
                    "SELECT COUNT(*) FROM signups WHERE ip_hash = ? AND created > ?",
                    (digest, now - 86400),
                ).fetchone()[0]
                if per_ip >= REGISTER_PER_IP_DAY:
                    self._error(429, "Too many keys from this address. Try again tomorrow.", "rate_limit_error")
                    return
                global_today = conn.execute(
                    "SELECT COUNT(*) FROM signups WHERE created > ?", (now - 86400,)
                ).fetchone()[0]
                if global_today >= REGISTER_GLOBAL_DAY:
                    self._error(429, "Daily key limit reached. Try again tomorrow.", "rate_limit_error")
                    return
                key_id = secrets.token_hex(8)
                raw, key_digest = new_key()
                prefix = raw[:18] + "..."
                conn.execute(
                    "INSERT INTO keys(id, name, prefix, hash, created, rpm, tier, token_limit, tokens_used, period_start, premium_until) "
                    "VALUES (?, ?, ?, ?, ?, ?, 'free', ?, 0, ?, 0)",
                    (key_id, name, prefix, key_digest, now, FREE_RPM, FREE_TOKEN_LIMIT, now),
                )
                conn.execute(
                    "INSERT INTO signups(ip_hash, key_id, created) VALUES (?, ?, ?)",
                    (digest, key_id, now),
                )
            self._send_json(
                201,
                {
                    "id": key_id,
                    "name": name,
                    "key": raw,
                    "prefix": prefix,
                    "rpm": FREE_RPM,
                    "tier": "free",
                    "token_limit": FREE_TOKEN_LIMIT,
                    "tokens_used": 0,
                    "tokens_remaining": FREE_TOKEN_LIMIT,
                    "model": MODEL_ID,
                    "warning": "Store this key now. The server keeps only its hash. Free keys have a monthly token limit.",
                },
            )
            return

        if path == "/v1/promo/activate":
            ip_hash = ip_digest(self._client_ip())
            if not promo_rate_ok(ip_hash):
                self._error(429, "Too many promo attempts. Try again in a minute.", "rate_limit_error")
                return
            code = str(body.get("code", "")).strip()
            device_id = str(body.get("device_id", "")).strip()[:200]
            device_hash = hashlib.sha256(("promo-device|" + device_id).encode()).hexdigest() if device_id else ""
            status, payload = activate_promo(code, device_hash, ip_hash)
            self._send_json(status, payload)
            return

        if path == "/admin/promo":
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            plan = str(body.get("plan", "")).strip().lower()
            try:
                count = int(body.get("count", 1))
                max_uses = int(body.get("max_uses", 1))
                expires_days = int(body.get("expires_days", 30))
                days = body.get("days")
                days = None if days is None else int(days)
            except Exception:  # noqa: BLE001
                self._error(400, "count, max_uses, days and expires_days must be integers.")
                return
            try:
                codes = create_promo_codes(
                    plan,
                    count,
                    max_uses=max_uses,
                    days=days,
                    expires_days=expires_days,
                    note=str(body.get("note", "")).strip(),
                )
            except ValueError as exc:
                self._error(400, str(exc))
                return
            except Exception as exc:  # noqa: BLE001
                self._error(500, str(exc), "server_error")
                return
            self._send_json(
                201,
                {
                    "codes": codes,
                    "plan": plan,
                    "count": len(codes),
                    "max_uses": max(1, min(max_uses, 1000)),
                    "days": PROMO_PLANS[plan]["days"] if days is None else max(0, min(days, 3650)),
                    "expires_days": max(0, min(expires_days, 3650)),
                },
            )
            return

        if path == "/admin/keys":
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            name = str(body.get("name", "devai-user")).strip()[:80] or "devai-user"
            try:
                rpm = int(body.get("rpm", 60))
            except Exception:  # noqa: BLE001
                rpm = 60
            rpm = max(1, min(rpm, 600))
            key_id = secrets.token_hex(8)
            raw, digest = new_key()
            prefix = raw[:18] + "..."
            with db() as conn:
                conn.execute(
                    "INSERT INTO keys(id, name, prefix, hash, created, rpm, tier, token_limit, tokens_used, period_start, premium_until) "
                    "VALUES (?, ?, ?, ?, ?, ?, 'admin', 0, 0, ?, 0)",
                    (key_id, name, prefix, digest, time.time(), rpm, time.time()),
                )
            self._send_json(
                201,
                {
                    "id": key_id,
                    "name": name,
                    "key": raw,
                    "prefix": prefix,
                    "rpm": rpm,
                    "tier": "admin",
                    "token_limit": 0,
                    "tokens_used": 0,
                    "model": MODEL_ID,
                    "warning": "Store this key now. The server keeps only its hash.",
                },
            )
            return

        if path.startswith("/admin/keys/") and path.endswith("/rotate"):
            if not self._is_admin():
                self._error(401, "Admin token required.", "authentication_error")
                return
            key_id = path.strip("/").split("/")[2]
            raw, digest = new_key()
            with db() as conn:
                cur = conn.execute(
                    "UPDATE keys SET hash = ?, prefix = ?, revoked = 0 WHERE id = ?",
                    (digest, raw[:18] + "...", key_id),
                )
            if cur.rowcount == 0:
                self._error(404, "Key not found.", "not_found")
                return
            self._send_json(200, {"id": key_id, "key": raw, "prefix": raw[:18] + "..."})
            return

        self._error(404, "Not found.", "not_found")



    def _upgrade_key(self, body: dict) -> None:
        key = self._key_row(record=False)
        if key is None:
            return
        if key["_tier"] == "admin":
            self._error(400, "Admin keys already have unlimited access.")
            return
        ip_hash = ip_digest(self._client_ip())
        if not promo_rate_ok(ip_hash):
            self._error(429, "Too many promo attempts. Try again in a minute.", "rate_limit_error")
            return
        code = str(body.get("code", "")).strip()
        device_id = str(body.get("device_id", "") or f"api:{key['id']}")[:200]
        device_hash = hashlib.sha256(("promo-device|" + device_id).encode()).hexdigest()
        status, payload = activate_promo(code, device_hash, ip_hash)
        if status != 200:
            self._send_json(status, payload)
            return
        days = int(payload.get("days") or 0)
        now = time.time()
        current_until = float(key.get("premium_until") or 0)
        base = max(now, current_until)
        premium_until = 4102444800 if days <= 0 else base + days * 86400
        with DB_LOCK, db() as conn:
            conn.execute(
                "UPDATE keys SET tier = 'premium', premium_until = ?, token_limit = ?, "
                "rpm = ?, tokens_used = 0, period_start = ? WHERE id = ?",
                (premium_until, PREMIUM_TOKEN_LIMIT, PREMIUM_RPM, now, key["id"]),
            )
        self._send_json(
            200,
            {
                **payload,
                "tier": "premium",
                "token_limit": PREMIUM_TOKEN_LIMIT,
                "tokens_used": 0,
                "tokens_remaining": PREMIUM_TOKEN_LIMIT,
                "premium_until": premium_until,
                "rpm": PREMIUM_RPM,
            },
        )

    def _chat(self, body: dict, key: dict) -> None:
        try:
            messages = clean_messages(body.get("messages"))
        except ValueError as exc:
            self._error(400, str(exc))
            return
        try:
            model = get_model()
        except Exception as exc:  # noqa: BLE001
            self._error(503, f"Model is not available: {exc}", "server_error")
            return

        stream = bool(body.get("stream", False))
        try:
            max_tokens = int(body.get("max_tokens", body.get("max_completion_tokens", 512)))
        except Exception:  # noqa: BLE001
            max_tokens = 512
        max_tokens = max(1, min(max_tokens, MAX_TOKENS))
        limit = int(key["_token_limit"])
        if limit > 0:
            remaining = max(1, limit - int(key["_tokens_used"]))
            max_tokens = min(max_tokens, remaining)
        try:
            temperature = float(body.get("temperature", 0.7))
        except Exception:  # noqa: BLE001
            temperature = 0.7
        temperature = max(0.0, min(temperature, 1.5))
        try:
            top_p = float(body.get("top_p", 0.95))
        except Exception:  # noqa: BLE001
            top_p = 0.95
        top_p = max(0.05, min(top_p, 1.0))
        stop = body.get("stop")
        if isinstance(stop, str):
            stop = [stop]
        if not isinstance(stop, list):
            stop = None

        if stream:
            self._stream_chat(model, messages, max_tokens, temperature, top_p, stop, key)
            return


        kwargs = {
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
            "top_p": top_p,
        }
        if stop:
            kwargs["stop"] = stop
        try:
            with GEN_LOCK:
                result = model.create_chat_completion(**kwargs)
        except Exception as exc:  # noqa: BLE001
            self._error(500, f"Generation failed: {exc}", "server_error")
            return

        choice = (result.get("choices") or [{}])[0]
        content = (choice.get("message") or {}).get("content") or ""
        usage = result.get("usage") or {}
        total_tokens = int(usage.get("total_tokens", 0) or 0)
        if total_tokens <= 0:
            total_tokens = estimate_message_tokens(messages) + estimate_tokens(content)
        charge_tokens(key["id"], total_tokens)
        self._send_json(
            200,
            {
                "id": "chatcmpl-" + secrets.token_hex(12),
                "object": "chat.completion",
                "created": int(time.time()),
                "model": MODEL_ID,
                "choices": [
                    {
                        "index": 0,
                        "message": {"role": "assistant", "content": content},
                        "finish_reason": choice.get("finish_reason", "stop"),
                    }
                ],
                "usage": {
                    "prompt_tokens": usage.get("prompt_tokens", 0),
                    "completion_tokens": usage.get("completion_tokens", 0),
                    "total_tokens": total_tokens,
                },
            },
            extra=self._quota_headers(key, total_tokens),
        )


    def _stream_chat(self, model, messages, max_tokens, temperature, top_p, stop, key: dict) -> None:
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream; charset=utf-8")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "close")
        self._cors()
        for name, value in self._quota_headers(key).items():
            self.send_header(name, value)
        self.end_headers()
        self.close_connection = True
        chunk_id = "chatcmpl-" + secrets.token_hex(12)
        created = int(time.time())

        kwargs = {
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
            "top_p": top_p,
            "stream": True,
        }
        if stop:
            kwargs["stop"] = stop


        completion_parts: list[str] = []
        try:
            with GEN_LOCK:
                for chunk in model.create_chat_completion(**kwargs):
                    choice = (chunk.get("choices") or [{}])[0]
                    delta = choice.get("delta") or {}
                    if not delta:
                        continue
                    content = delta.get("content")
                    if isinstance(content, str):
                        completion_parts.append(content)
                    payload = {
                        "id": chunk_id,
                        "object": "chat.completion.chunk",
                        "created": created,
                        "model": MODEL_ID,
                        "choices": [
                            {
                                "index": 0,
                                "delta": delta,
                                "finish_reason": choice.get("finish_reason"),
                            }
                        ],
                    }
                    line = "data: " + json.dumps(payload, ensure_ascii=False) + "\n\n"
                    self.wfile.write(line.encode("utf-8"))
                    self.wfile.flush()

        except (BrokenPipeError, ConnectionResetError):
            return

        except Exception as exc:  # noqa: BLE001
            try:
                err = {"error": {"message": str(exc), "type": "server_error"}}
                self.wfile.write(("data: " + json.dumps(err, ensure_ascii=False) + "\n\n").encode())
                self.wfile.flush()
            except Exception:  # noqa: BLE001
                pass
            return
        finally:
            charge_tokens(
                key["id"],
                estimate_message_tokens(messages) + estimate_tokens("".join(completion_parts)),
            )

        self.wfile.write(("data: " + chr(91) + "DONE" + chr(93) + "\n\n").encode())
        self.wfile.flush()

class DevAIServer(ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True

def run() -> None:
    init_db()
    print("DevAI Core API v" + SERVER_VERSION + " on " + HOST + ":" + str(PORT), flush=True)
    try:
        get_model()
        print("Model ready: " + str(DEFAULT_MODEL), flush=True)
    except Exception as exc:
        print("Model unavailable: " + str(exc), flush=True)
    DevAIServer((HOST, PORT), Handler).serve_forever()


if __name__ == "__main__":
    run()
