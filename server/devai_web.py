"""Safe page fetching, link extraction and heuristic URL scanning.

Everything here is stdlib-only so the API server can run without extra
dependencies. Network access is restricted to public HTTP(S) hosts: private,
loopback and link-local addresses are rejected to prevent SSRF.
"""
from __future__ import annotations

import gzip
import html
import ipaddress
import re
import socket
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
import urllib.robotparser
import zlib
from html.parser import HTMLParser

USER_AGENT = (
    "DevAI-Reader/1.0 (+https://karishka-sr.github.io/devai-studio/; "
    "autonomous learning and safety checks)"
)
MAX_BYTES = 1_500_000
MAX_REDIRECTS = 4
ROBOTS_CACHE: dict[str, tuple[float, bool]] = {}

URL_RE = re.compile(
    r"(?i)\b(?:https?://|www\.|t\.me/|telegram\.me/)[^\s<>\"'`]+"
)
TRAILING = ".,;:!?)]}>»”’"

SUSPICIOUS_TLDS = {
    "zip", "mov", "top", "xyz", "click", "link", "gq", "tk", "ml", "cf", "ga",
    "work", "support", "rest", "fit", "cam", "surf", "monster", "buzz", "cyou",
    "icu", "lol", "quest", "sbs", "cfd", "autos", "boats", "homes", "bond",
    "beauty", "makeup", "skin", "hair", "mom", "dad", "pics", "sexy", "wang",
}
BLOCKED_HOSTS = {
    "localhost", "localhost.localdomain", "metadata.google.internal",
    "metadata", "instance-data",
}
SKIP_SCHEMES = {"javascript", "data", "file", "ftp", "mailto", "tel", "about", "chrome"}
SKIP_EXTENSIONS = {
    ".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif", ".ico", ".bmp",
    ".mp3", ".mp4", ".webm", ".ogg", ".wav", ".avi", ".mov", ".mkv",
    ".woff", ".woff2", ".ttf", ".otf", ".eot",
    ".css", ".js", ".map", ".wasm",
}
SKIP_PATH_PARTS = (
    "/login", "/logout", "/signin", "/signup", "/cart", "/checkout",
    "/account/delete", "/unsubscribe", "/admin", "/wp-admin", "/api/",
)


class UnsafeUrl(ValueError):
    """Raised when a URL must not be fetched by the server."""


class _RedirectRecorder(urllib.request.HTTPRedirectHandler):
    def __init__(self) -> None:
        super().__init__()
        self.chain: list[str] = []

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        newurl = urllib.parse.urljoin(req.full_url, newurl)
        if not url_is_safe(newurl):
            raise UnsafeUrl("redirect to a non-public address blocked")
        self.chain.append(newurl)
        if len(self.chain) > MAX_REDIRECTS:
            raise UnsafeUrl("too many redirects")
        return super().redirect_request(req, fp, code, msg, headers, newurl)


class PageParser(HTMLParser):
    SKIP = {"script", "style", "noscript", "svg", "template", "canvas", "iframe"}
    BLOCK = {
        "p", "div", "br", "li", "tr", "section", "article", "header", "footer",
        "main", "aside", "nav", "form", "table", "ul", "ol", "pre", "blockquote",
    }
    HEADINGS = {"h1", "h2", "h3", "h4"}

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.title = ""
        self.description = ""
        self.text_parts: list[str] = []
        self.headings: list[str] = []
        self.links: list[str] = []
        self._skip_depth = 0
        self._in_title = False
        self._title_parts: list[str] = []
        self._heading_parts: list[str] = []
        self._in_heading = False
        self._current_heading_tag = ""

    def handle_starttag(self, tag: str, attrs) -> None:
        tag = tag.lower()
        attrs_map = {k.lower(): (v or "") for k, v in attrs}
        if tag == "meta":
            name = attrs_map.get("property") or attrs_map.get("name")
            content = attrs_map.get("content", "")
            if name and content:
                if name.lower() in {"og:title", "twitter:title"} and not self.title:
                    self.title = html.unescape(content).strip()
                if name.lower() in {"og:description", "description", "twitter:description"}:
                    self.description = html.unescape(content).strip()
            return
        if tag in self.SKIP:
            self._skip_depth += 1
            return
        if tag == "title":
            self._in_title = True
        if tag == "a" and attrs_map.get("href"):
            self.links.append(attrs_map["href"])
        if tag in self.HEADINGS:
            self._in_heading = True
            self._current_heading_tag = tag
            self._heading_parts = []
        if tag in self.BLOCK or tag in self.HEADINGS:
            self.text_parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        tag = tag.lower()
        if tag in self.SKIP and self._skip_depth:
            self._skip_depth -= 1
            return
        if tag == "title":
            self._in_title = False
            if not self.title:
                self.title = " ".join(self._title_parts).strip()
        if tag == "a" and self._in_heading:
            pass
        if tag in self.HEADINGS and self._in_heading and tag == self._current_heading_tag:
            heading = clean_text(" ".join(self._heading_parts))
            if heading:
                self.headings.append(heading)
            self._in_heading = False
        if tag in self.BLOCK or tag in self.HEADINGS:
            self.text_parts.append("\n")

    def handle_data(self, data: str) -> None:
        if self._skip_depth:
            return
        if self._in_title:
            self._title_parts.append(data)
            return
        if self._in_heading:
            self._heading_parts.append(data)
        self.text_parts.append(data)


def clean_text(value: str, limit: int = 80_000) -> str:
    text = html.unescape(str(value or ""))
    text = text.replace("\u00a0", " ")
    text = re.sub(r"[ \t\f\v]+", " ", text)
    text = re.sub(r"\n\s*\n\s*\n+", "\n\n", text)
    lines = [line.strip() for line in text.splitlines()]
    text = "\n".join(line for line in lines if line)
    return text[:limit].strip()


def normalize_url(raw: str) -> str | None:
    value = html.unescape(str(raw or "").strip())
    value = value.rstrip(TRAILING)
    if not value:
        return None
    if value.startswith("//"):
        value = "https:" + value
    if re.match(r"(?i)^(www\.|t\.me/|telegram\.me/)", value):
        value = "https://" + value
    parsed = urllib.parse.urlsplit(value)
    if parsed.scheme.lower() in SKIP_SCHEMES:
        return None
    if parsed.scheme.lower() not in {"http", "https"}:
        return None
    if not parsed.hostname:
        return None
    try:
        host = parsed.hostname.encode("idna").decode("ascii").lower().strip(".")
    except Exception:
        return None
    if not host or host in BLOCKED_HOSTS:
        return None
    if parsed.port and parsed.port not in {80, 443}:
        return None
    path = parsed.path or "/"
    query = parsed.query
    # Drop fragments and common tracking parameters.
    if query:
        pairs = []
        for part in query.split("&"):
            key = part.split("=", 1)[0].lower()
            if key.startswith("utm_") or key in {"fbclid", "gclid", "yclid", "mc_cid", "mc_eid"}:
                continue
            pairs.append(part)
        query = "&".join(pairs)
    return urllib.parse.urlunsplit((parsed.scheme.lower(), host, path, query, ""))


def telegram_preview_url(url: str) -> str | None:
    parsed = urllib.parse.urlsplit(url)
    host = (parsed.hostname or "").lower()
    if host not in {"t.me", "telegram.me", "www.t.me"}:
        return None
    parts = [p for p in parsed.path.split("/") if p]
    if not parts or parts[0] == "s":
        return None
    name = parts[0]
    if name.startswith("+") or name in {"share", "joinchat", "addstickers", "proxy"}:
        return None
    return "https://t.me/s/" + name


def _resolve_public(host: str) -> bool:
    if host in BLOCKED_HOSTS or host.endswith((".local", ".internal", ".home", ".lan")):
        return False
    try:
        infos = socket.getaddrinfo(host, 443, type=socket.SOCK_STREAM)
    except Exception:
        return False
    if not infos:
        return False
    for info in infos:
        addr = info[4][0]
        try:
            ip = ipaddress.ip_address(addr)
        except ValueError:
            return False
        if not ip.is_global or ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
            return False
    return True


def url_is_safe(url: str | None) -> bool:
    if not url:
        return False
    normalized = normalize_url(url)
    if not normalized:
        return False
    parsed = urllib.parse.urlsplit(normalized)
    if parsed.username or parsed.password:
        return False
    return _resolve_public(parsed.hostname or "")


def extract_urls(text: str, limit: int = 12) -> list[str]:
    found: list[str] = []
    for match in URL_RE.finditer(str(text or "")):
        url = normalize_url(match.group(0))
        if url and url not in found:
            found.append(url)
        if len(found) >= limit:
            break
    return found


def robots_allowed(url: str, timeout: float = 4.0) -> bool:
    parsed = urllib.parse.urlsplit(url)
    host = parsed.hostname or ""
    cached = ROBOTS_CACHE.get(host)
    if cached and time.time() - cached[0] < 6 * 3600:
        allowed = cached[1]
        return allowed if allowed else False
    robots = urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, "/robots.txt", "", ""))
    parser = urllib.robotparser.RobotFileParser()
    try:
        req = urllib.request.Request(robots, headers={"User-Agent": USER_AGENT, "Accept": "text/plain"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            parser.parse(resp.read(64_000).decode("utf-8", "replace").splitlines())
    except Exception:
        # A missing or broken robots.txt is treated as permissive, as browsers do.
        ROBOTS_CACHE[host] = (time.time(), True)
        return True
    allowed = parser.can_fetch(USER_AGENT, url)
    ROBOTS_CACHE[host] = (time.time(), allowed)
    return allowed


def _decode_body(raw: bytes, encoding: str) -> bytes:
    encoding = (encoding or "").lower()
    if "gzip" in encoding:
        try:
            return gzip.decompress(raw)
        except Exception:
            return raw
    if "deflate" in encoding:
        try:
            return zlib.decompress(raw)
        except Exception:
            return raw
    return raw


def fetch_url(
    url: str,
    timeout: float = 7.0,
    max_bytes: int = MAX_BYTES,
) -> dict:
    normalized = normalize_url(url)
    if not normalized or not url_is_safe(normalized):
        raise UnsafeUrl("URL is not allowed")
    if not robots_allowed(normalized, timeout=min(timeout, 4.0)):
        raise UnsafeUrl("robots.txt disallows this URL")
    redirects = _RedirectRecorder()
    opener = urllib.request.build_opener(
        redirects,
        urllib.request.HTTPSHandler(context=ssl.create_default_context()),
    )
    req = urllib.request.Request(
        normalized,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "text/html,application/xhtml+xml,text/plain,application/json;q=0.8,*/*;q=0.5",
            "Accept-Language": "ru,en;q=0.8",
            "Accept-Encoding": "gzip, deflate",
        },
    )
    started = time.time()
    try:
        with opener.open(req, timeout=timeout) as resp:
            status = int(getattr(resp, "status", 200) or 200)
            final_url = resp.geturl()
            content_type = resp.headers.get("Content-Type", "")
            raw = resp.read(max_bytes + 1)
            raw = _decode_body(raw, resp.headers.get("Content-Encoding", ""))
    except urllib.error.HTTPError as exc:
        return {
            "url": normalized,
            "final_url": exc.geturl() or normalized,
            "status": int(exc.code),
            "content_type": exc.headers.get("Content-Type", "") if exc.headers else "",
            "body": "",
            "truncated": False,
            "error": "HTTP " + str(exc.code),
            "elapsed": time.time() - started,
            "redirects": redirects.chain,
        }
    except Exception as exc:
        return {
            "url": normalized,
            "final_url": normalized,
            "status": 0,
            "content_type": "",
            "body": "",
            "truncated": False,
            "error": str(exc),
            "elapsed": time.time() - started,
            "redirects": redirects.chain,
        }
    truncated = len(raw) > max_bytes
    raw = raw[:max_bytes]
    return {
        "url": normalized,
        "final_url": final_url,
        "status": status,
        "content_type": content_type,
        "body": raw,
        "truncated": truncated,
        "error": "",
        "elapsed": time.time() - started,
        "redirects": redirects.chain,
    }


def _charset(content_type: str) -> str:
    match = re.search(r"(?i)charset=([\w-]+)", content_type or "")
    return match.group(1) if match else "utf-8"


def parse_page(fetch: dict) -> dict:
    raw: bytes = fetch.get("body") or b""
    content_type = (fetch.get("content_type") or "").lower()
    if "html" in content_type or raw.lstrip().startswith((b"<!doctype", b"<html")):
        text = raw.decode(_charset(content_type), "replace")
        parser = PageParser()
        try:
            parser.feed(text)
        except Exception:
            pass
        title = clean_text(parser.title, 300)
        description = clean_text(parser.description, 1_000)
        page_text = clean_text(" ".join(parser.text_parts), 120_000)
        links = []
        for href in parser.links:
            resolved = normalize_url(urllib.parse.urljoin(fetch.get("final_url") or fetch.get("url") or "", href))
            if resolved and resolved not in links:
                links.append(resolved)
        headings = parser.headings[:40]
    elif any(x in content_type for x in ("text/", "json", "xml", "javascript")):
        page_text = clean_text(raw.decode(_charset(content_type), "replace"), 120_000)
        title = page_text.splitlines()[0][:300] if page_text else ""
        description = page_text[:500]
        links = []
        headings = []
    else:
        page_text = ""
        title = ""
        description = ""
        links = []
        headings = []
    if not title:
        title = clean_text(fetch.get("url", ""), 300)
    if (fetch.get("final_url") or "").startswith("https://t.me/s/"):
        title = re.sub(r"^Telegram: Launch ", "Telegram ", title).strip()
        if "Launch @" in description or not description:
            description = "Открыть в Telegram: " + (fetch.get("url") or "")
    return {
        **fetch,
        "title": title,
        "description": description,
        "text": page_text,
        "links": links[:80],
        "headings": headings,
    }


def page_chunks(page: dict, limit: int = 8, chunk_size: int = 1_400) -> list[str]:
    text = clean_text(page.get("text") or "")
    if not text:
        return []
    paragraphs = [p.strip() for p in re.split(r"\n+", text) if len(p.strip()) >= 80]
    if not paragraphs:
        paragraphs = [text]
    chunks: list[str] = []
    current = ""
    for paragraph in paragraphs:
        if len(current) + len(paragraph) + 2 <= chunk_size:
            current = (current + "\n\n" + paragraph).strip()
        else:
            if current:
                chunks.append(current)
            current = paragraph[:chunk_size]
        if len(chunks) >= limit:
            break
    if current and len(chunks) < limit:
        chunks.append(current)
    return chunks


def analyze_url(url: str, page: dict | None = None) -> dict:
    findings: list[dict] = []
    score = 0

    def add(fid: str, severity: str, title: str, detail: str, points: int) -> None:
        nonlocal score
        score = min(100, score + points)
        findings.append({"id": fid, "severity": severity, "title": title, "detail": detail})

    normalized = normalize_url(url)
    if not normalized:
        return {
            "kind": "url", "target": str(url), "verdict": "unknown", "score": 0,
            "findings": [{"id": "invalid-url", "severity": "info", "title": "Некорректная ссылка",
                          "detail": "Ссылка не похожа на публичный HTTP(S)-адрес."}],
        }
    parsed = urllib.parse.urlsplit(normalized)
    host = parsed.hostname or ""
    if parsed.scheme != "https":
        add("http", "low", "Нет HTTPS", "Соединение не шифруется.", 10)
    if re.fullmatch(r"[\d.]+", host) or ":" in host:
        add("ip-host", "medium", "Ссылка на IP-адрес", "Ссылки на IP часто используют фишеры.", 20)
    if host.startswith("xn--") or ".xn--" in host:
        add("punycode", "medium", "Punycode-домен", "Домен может маскировать кириллические буквы.", 20)
    if host.count(".") >= 4:
        add("subdomains", "low", "Много поддоменов", "Проверьте, совпадает ли основной домен.", 8)
    if len(normalized) > 220:
        add("long-url", "low", "Длинная ссылка", "Длинные ссылки часто скрывают параметры.", 6)
    if "@" in parsed.netloc:
        add("userinfo", "high", "Подмена адреса через @", "Всё до @ может быть маскировкой.", 35)
    tld = host.rsplit(".", 1)[-1] if "." in host else ""
    if tld in SUSPICIOUS_TLDS:
        add("tld", "medium", "Домен из рискованной зоны", "Зона ." + tld + " часто используется в атаках.", 18)
    blocked = not url_is_safe(normalized)
    if blocked:
        add("unreachable", "medium", "Ссылка не проверена",
            "Адрес ведёт на приватный или непубличный хост; открытие заблокировано.", 20)

    if page is None and not blocked:
        try:
            page = parse_page(fetch_url(normalized, timeout=6))
        except Exception as exc:
            add("fetch-error", "medium", "Сайт не открылся", str(exc), 12)
            page = None

    if page:
        if page.get("status") and int(page["status"]) >= 400:
            add("http-status", "medium", "Ошибка сайта", "HTTP " + str(page["status"]), 12)
        if page.get("redirects"):
            final_host = urllib.parse.urlsplit(page.get("final_url") or "").hostname or ""
            if final_host and final_host != host:
                add("redirect", "medium", "Переход на другой домен",
                    "Редирект: " + host + " → " + final_host, 18)
        html_text = (page.get("body") or b"")[:400_000].decode("utf-8", "replace").lower()
        forms = re.findall(r"<form[^>]+action=[\"']?([^\"'\s>]+)", html_text)
        if re.search(r"<input[^>]+type=[\"']?password", html_text):
            for action in forms:
                action_host = urllib.parse.urlsplit(action).hostname or ""
                if action_host and action_host != host:
                    add("credential-form", "high", "Форма пароля отправляет данные на другой домен",
                        "Похоже на фишинг: " + action_host, 45)
                    break
        for src in re.findall(r"<iframe[^>]+src=[\"']?([^\"'\s>]+)", html_text):
            src_host = urllib.parse.urlsplit(src).hostname or ""
            if src_host and src_host != host:
                add("external-iframe", "low", "Внешний iframe", "Сторонний iframe: " + src_host, 5)
                break
        if re.search(r"(?i)(eval\s*\(|atob\s*\(|unescape\s*\(|document\.write\s*\()", html_text):
            add("obfuscated-js", "medium", "Обфусцированный JavaScript",
                "Скрипт может скрывать вредоносную логику.", 15)
        if re.search(r"(?i)(meta[^>]+http-equiv=[\"']?refresh[^>]+url=https?://)", html_text):
            add("meta-refresh", "low", "Автопереход", "Страница перенаправляет браузер.", 8)
        if re.search(r"(?i)\.(exe|scr|bat|cmd|msi|apk|jar|vbs)(?:[\"'?#]|$)", html_text):
            add("download", "medium", "Ссылка на исполняемый файл",
                "Не запускайте скачанный файл без проверки антивирусом.", 15)
        for phrase, points in (
            ("seed phrase", 35), ("private key", 25), ("wallet connect", 25),
            ("telegram premium free", 25), ("подтвердите аккаунт", 20),
            ("введите пароль", 15), ("ваш аккаунт заблокирован", 20),
        ):
            if phrase in html_text:
                add("phishing-phrase", "high", "Фишинговая фраза",
                    "На странице есть «" + phrase + "».", points)
                break
    if "t.me" in host or "telegram.me" in host:
        findings.append({
            "id": "telegram", "severity": "info", "title": "Ссылка Telegram",
            "detail": "Открывается в Telegram. Содержимое чата недоступно без приложения.",
        })
    verdict = "malicious" if score >= 70 else "suspicious" if score >= 30 else "clean"
    return {
        "kind": "url",
        "target": normalized,
        "final_url": page.get("final_url") if page else normalized,
        "title": page.get("title") if page else "",
        "verdict": verdict,
        "score": score,
        "findings": findings,
        "checked_at": int(time.time()),
        "page": {
            "title": page.get("title", "") if page else "",
            "description": page.get("description", "") if page else "",
            "text": (page.get("text") or "")[:3_000],
        } if page else None,
    }
