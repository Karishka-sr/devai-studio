#!/usr/bin/env python3
"""DevAI Stars affiliate bot.

Sells DevAI Premium for Telegram Stars, tracks referral partners, and keeps a
payout queue. The bot never invents Stars: sales are paid through Telegram
Stars invoices, and partner withdrawals are sent manually by an admin.

Run:
    export TELEGRAM_BOT_TOKEN="..."
    export TELEGRAM_ADMIN_IDS="123456789"
    export DEVAI_ADMIN_TOKEN="$(cat server/data/admin_token)"
    python3 server/stars_bot.py

The Mini App is served from docs/mini and uses STARS_MINI_APP_URL
(default: GitHub Pages). Promo codes are created only by the server and
require DEVAI_API_BASE and DEVAI_ADMIN_TOKEN.
"""

from __future__ import annotations

import json
import logging
import os
import secrets
import sqlite3
import string
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

BASE_DIR = Path(__file__).resolve().parent
DB_PATH = Path(os.environ.get("STARS_BOT_DB", BASE_DIR / "data" / "stars_bot.db"))
TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "").strip()
PUBLIC_URL = os.environ.get("STARS_PUBLIC_URL", "https://karishka-sr.github.io/devai-studio/stars/")
MINI_APP_URL = os.environ.get("STARS_MINI_APP_URL", "https://karishka-sr.github.io/devai-studio/mini/")
DEVAI_API_BASE = os.environ.get("DEVAI_API_BASE", "https://work-2-nizvyfgvcqicrkif.prod-runtime.all-hands.dev").rstrip("/")
DEVAI_ADMIN_TOKEN = os.environ.get("DEVAI_ADMIN_TOKEN", "").strip()
COMMISSION_RATE = min(max(float(os.environ.get("STARS_COMMISSION_RATE", "0.20")), 0.0), 1.0)
MIN_PAYOUT = max(int(os.environ.get("STARS_MIN_PAYOUT", "100")), 1)
FREE_PROMO_DAYS = max(1, min(int(os.environ.get("STARS_FREE_PROMO_DAYS", "2")), 30))
FREE_PROMO_COOLDOWN_DAYS = max(1, min(int(os.environ.get("STARS_FREE_PROMO_COOLDOWN_DAYS", "7")), 365))
FREE_PROMO_CODE_TTL_DAYS = max(1, min(int(os.environ.get("STARS_FREE_PROMO_CODE_TTL_DAYS", "30")), 3650))
BOT_USERNAME = os.environ.get("TELEGRAM_BOT_USERNAME", "").lstrip("@")


def _admin_ids() -> set[int]:
    raw = os.environ.get("TELEGRAM_ADMIN_IDS", "")
    result: set[int] = set()
    for part in raw.replace(";", ",").split(","):
        part = part.strip()
        if part.lstrip("-").isdigit():
            result.add(int(part))
    return result


ADMIN_IDS = _admin_ids()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%H:%M:%S",
)
log = logging.getLogger("stars-bot")


@dataclass(frozen=True)
class Plan:
    id: str
    title: str
    days: str
    stars: int
    days_value: int


PLANS: dict[str, Plan] = {
    "week": Plan("week", "Неделя", "7 дней", 75, 7),
    "month": Plan("month", "Месяц", "30 дней", 249, 30),
    "year": Plan("year", "Год", "365 дней", 1490, 365),
    "lifetime": Plan("lifetime", "Навсегда", "навсегда", 4990, 0),
}

API_PLANS: dict[str, Plan] = {
    "api_week": Plan("api_week", "API · Неделя", "7 дней", 75, 7),
    "api_month": Plan("api_month", "API · Месяц", "30 дней", 249, 30),
    "api_year": Plan("api_year", "API · Год", "365 дней", 1490, 365),
    "api_lifetime": Plan("api_lifetime", "API · Навсегда", "навсегда", 4990, 0),
}

ALL_PLANS: dict[str, Plan] = {**PLANS, **API_PLANS}

SCHEMA = """
CREATE TABLE IF NOT EXISTS users (
    telegram_id INTEGER PRIMARY KEY,
    username TEXT NOT NULL DEFAULT '',
    first_name TEXT NOT NULL DEFAULT '',
    referred_by INTEGER,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS partners (
    telegram_id INTEGER PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    balance INTEGER NOT NULL DEFAULT 0,
    earned INTEGER NOT NULL DEFAULT 0,
    paid INTEGER NOT NULL DEFAULT 0,
    starts INTEGER NOT NULL DEFAULT 0,
    sales INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id TEXT PRIMARY KEY,
    buyer_id INTEGER NOT NULL,
    plan TEXT NOT NULL,
    amount INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    partner_id INTEGER,
    commission INTEGER NOT NULL DEFAULT 0,
    charge_id TEXT,
    created_at TEXT NOT NULL,
    paid_at TEXT
);

CREATE TABLE IF NOT EXISTS payouts (
    id TEXT PRIMARY KEY,
    partner_id INTEGER NOT NULL,
    amount INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL,
    paid_at TEXT,
    note TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS free_promo_claims (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_id INTEGER NOT NULL,
    code TEXT NOT NULL DEFAULT '',
    claimed_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_referred_by ON users(referred_by);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_payouts_status ON payouts(status);
CREATE INDEX IF NOT EXISTS idx_free_promo_claims_user ON free_promo_claims(telegram_id, id);
"""


def now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def connect() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys = ON")
    return connection


def init_db() -> None:
    with connect() as connection:
        connection.executescript(SCHEMA)


def fmt_stars(value: int) -> str:
    return f"{value:,}".replace(",", " ")


def ensure_user(telegram_id: int, username: str, first_name: str, ref_code: str | None = None) -> sqlite3.Row:
    code = (ref_code or "").strip().upper()
    with connect() as connection:
        user = connection.execute("SELECT * FROM users WHERE telegram_id = ?", (telegram_id,)).fetchone()
        partner = None
        if code:
            partner = connection.execute("SELECT telegram_id FROM partners WHERE code = ?", (code,)).fetchone()

        if user is None:
            partner_id = partner["telegram_id"] if partner and partner["telegram_id"] != telegram_id else None
            connection.execute(
                "INSERT INTO users (telegram_id, username, first_name, referred_by, created_at) VALUES (?, ?, ?, ?, ?)",
                (telegram_id, username or "", first_name or "", partner_id, now()),
            )
            if partner_id:
                connection.execute("UPDATE partners SET starts = starts + 1 WHERE telegram_id = ?", (partner_id,))
        else:
            connection.execute(
                "UPDATE users SET username = ?, first_name = ? WHERE telegram_id = ?",
                (username or "", first_name or "", telegram_id),
            )
            if user["referred_by"] is None and partner and partner["telegram_id"] != telegram_id:
                connection.execute("UPDATE users SET referred_by = ? WHERE telegram_id = ?", (partner["telegram_id"], telegram_id))
                connection.execute("UPDATE partners SET starts = starts + 1 WHERE telegram_id = ?", (partner["telegram_id"],))

        return connection.execute("SELECT * FROM users WHERE telegram_id = ?", (telegram_id,)).fetchone()


def _as_utc(value: str) -> datetime:
    parsed = datetime.fromisoformat(value)
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def reserve_free_promo(telegram_id: int) -> tuple[int | None, datetime | None]:
    with connect() as connection:
        connection.execute("BEGIN IMMEDIATE")
        last = connection.execute(
            "SELECT * FROM free_promo_claims WHERE telegram_id = ? AND code <> '' ORDER BY id DESC LIMIT 1",
            (telegram_id,),
        ).fetchone()
        if last:
            next_at = _as_utc(last["claimed_at"]) + timedelta(days=FREE_PROMO_COOLDOWN_DAYS)
            if next_at > datetime.now(timezone.utc):
                return None, next_at
        cursor = connection.execute(
            "INSERT INTO free_promo_claims (telegram_id, code, claimed_at) VALUES (?, '', ?)",
            (telegram_id, now()),
        )
        return int(cursor.lastrowid), None


def finish_free_promo(claim_id: int, code: str) -> None:
    with connect() as connection:
        connection.execute("UPDATE free_promo_claims SET code = ? WHERE id = ?", (code, claim_id))


def cancel_free_promo(claim_id: int) -> None:
    with connect() as connection:
        connection.execute("DELETE FROM free_promo_claims WHERE id = ?", (claim_id,))


def fmt_remaining(target: datetime) -> str:
    seconds = max(0, int((target - datetime.now(timezone.utc)).total_seconds()))
    days, remainder = divmod(seconds, 86400)
    hours = remainder // 3600
    if days:
        return f"{days} дн. {hours} ч"
    minutes = max(1, remainder // 60)
    return f"{hours} ч {minutes} мин" if hours else f"{minutes} мин"


def get_partner(telegram_id: int) -> sqlite3.Row | None:
    with connect() as connection:
        return connection.execute("SELECT * FROM partners WHERE telegram_id = ?", (telegram_id,)).fetchone()


def ensure_partner(telegram_id: int) -> sqlite3.Row:
    existing = get_partner(telegram_id)
    if existing:
        return existing

    alphabet = string.ascii_uppercase + string.digits
    with connect() as connection:
        for _ in range(20):
            code = "".join(secrets.choice(alphabet) for _ in range(7))
            try:
                connection.execute(
                    "INSERT INTO partners (telegram_id, code, created_at) VALUES (?, ?, ?)",
                    (telegram_id, code, now()),
                )
                return connection.execute("SELECT * FROM partners WHERE telegram_id = ?", (telegram_id,)).fetchone()
            except sqlite3.IntegrityError:
                continue
    raise RuntimeError("Не удалось создать партнёрский код")


def partner_link(code: str) -> str:
    if not BOT_USERNAME:
        return ""
    return f"https://t.me/{BOT_USERNAME}?start=ref_{code}"


def create_order(buyer_id: int, plan_id: str) -> str:
    plan = ALL_PLANS[plan_id]
    order_id = f"DVS-{plan.id.upper()}-{int(time.time()):X}{secrets.token_hex(2).upper()}"
    with connect() as connection:
        connection.execute(
            "INSERT INTO orders (id, buyer_id, plan, amount, status, created_at) VALUES (?, ?, ?, ?, 'pending', ?)",
            (order_id, buyer_id, plan.id, plan.stars, now()),
        )
    return order_id


def get_order(order_id: str) -> sqlite3.Row | None:
    with connect() as connection:
        return connection.execute("SELECT * FROM orders WHERE id = ?", (order_id,)).fetchone()


def mark_order_paid(order_id: str, charge_id: str) -> dict[str, Any] | None:
    with connect() as connection:
        order = connection.execute("SELECT * FROM orders WHERE id = ?", (order_id,)).fetchone()
        if order is None or order["status"] == "paid":
            return None

        buyer = connection.execute("SELECT * FROM users WHERE telegram_id = ?", (order["buyer_id"],)).fetchone()
        partner_id = buyer["referred_by"] if buyer else None
        commission = 0
        if partner_id and partner_id != order["buyer_id"]:
            partner = connection.execute("SELECT * FROM partners WHERE telegram_id = ?", (partner_id,)).fetchone()
            if partner:
                commission = int(round(order["amount"] * COMMISSION_RATE))
                if commission > 0:
                    connection.execute(
                        "UPDATE partners SET balance = balance + ?, earned = earned + ?, sales = sales + 1 WHERE telegram_id = ?",
                        (commission, commission, partner_id),
                    )
            else:
                partner_id = None

        connection.execute(
            "UPDATE orders SET status = 'paid', partner_id = ?, commission = ?, charge_id = ?, paid_at = ? WHERE id = ?",
            (partner_id, commission, charge_id, now(), order_id),
        )
        result = connection.execute("SELECT * FROM orders WHERE id = ?", (order_id,)).fetchone()
        return dict(result)


def request_payout(telegram_id: int, amount: int) -> tuple[str | None, str]:
    if amount < MIN_PAYOUT:
        return None, f"Минимальная сумма вывода — {fmt_stars(MIN_PAYOUT)} ⭐."
    with connect() as connection:
        partner = connection.execute("SELECT * FROM partners WHERE telegram_id = ?", (telegram_id,)).fetchone()
        if partner is None:
            return None, "Сначала открой партнёрскую программу."
        if partner["balance"] < amount:
            return None, f"На балансе только {fmt_stars(partner['balance'])} ⭐."
        payout_id = f"PO-{int(time.time()):X}{secrets.token_hex(2).upper()}"
        connection.execute(
            "UPDATE partners SET balance = balance - ? WHERE telegram_id = ?",
            (amount, telegram_id),
        )
        connection.execute(
            "INSERT INTO payouts (id, partner_id, amount, status, created_at) VALUES (?, ?, ?, 'pending', ?)",
            (payout_id, telegram_id, amount, now()),
        )
        return payout_id, ""


def pending_payouts(limit: int = 20) -> list[sqlite3.Row]:
    with connect() as connection:
        return connection.execute(
            "SELECT * FROM payouts WHERE status = 'pending' ORDER BY created_at LIMIT ?", (limit,)
        ).fetchall()


def mark_payout_paid(payout_id: str) -> sqlite3.Row | None:
    with connect() as connection:
        payout = connection.execute(
            "SELECT * FROM payouts WHERE id = ? AND status = 'pending'", (payout_id,)
        ).fetchone()
        if payout is None:
            return None
        connection.execute(
            "UPDATE payouts SET status = 'paid', paid_at = ? WHERE id = ?", (now(), payout_id)
        )
        connection.execute(
            "UPDATE partners SET paid = paid + ? WHERE telegram_id = ?",
            (payout["amount"], payout["partner_id"]),
        )
        return payout


def reject_payout(payout_id: str) -> sqlite3.Row | None:
    with connect() as connection:
        payout = connection.execute(
            "SELECT * FROM payouts WHERE id = ? AND status = 'pending'", (payout_id,)
        ).fetchone()
        if payout is None:
            return None
        connection.execute("UPDATE payouts SET status = 'rejected' WHERE id = ?", (payout_id,))
        connection.execute(
            "UPDATE partners SET balance = balance + ? WHERE telegram_id = ?",
            (payout["amount"], payout["partner_id"]),
        )
        return payout


def admin_stats() -> dict[str, Any]:
    with connect() as connection:
        users = connection.execute("SELECT COUNT(*) AS n FROM users").fetchone()["n"]
        partners = connection.execute("SELECT COUNT(*) AS n FROM partners").fetchone()["n"]
        paid_orders = connection.execute("SELECT COUNT(*) AS n, COALESCE(SUM(amount), 0) AS s FROM orders WHERE status = 'paid'").fetchone()
        pending = connection.execute("SELECT COUNT(*) AS n, COALESCE(SUM(amount), 0) AS s FROM payouts WHERE status = 'pending'").fetchone()
        return {
            "users": users,
            "partners": partners,
            "paid_orders": paid_orders["n"],
            "paid_stars": paid_orders["s"],
            "pending_payouts": pending["n"],
            "pending_stars": pending["s"],
        }


def recent_orders(limit: int = 10) -> list[sqlite3.Row]:
    with connect() as connection:
        return connection.execute("SELECT * FROM orders ORDER BY created_at DESC LIMIT ?", (limit,)).fetchall()


def api(method: str, payload: dict[str, Any] | None = None) -> Any:
    if not TOKEN:
        raise RuntimeError("TELEGRAM_BOT_TOKEN не задан")
    data = json.dumps(payload or {}, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        f"https://api.telegram.org/bot{TOKEN}/{method}",
        data=data,
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=40) as response:
            body = response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")
        try:
            description = json.loads(body).get("description", body)
        except json.JSONDecodeError:
            description = body
        raise RuntimeError(description) from error
    result = json.loads(body)
    if not result.get("ok"):
        raise RuntimeError(result.get("description", "Telegram API error"))
    return result.get("result")


def http_json(url: str, payload: dict[str, Any], headers: dict[str, str] | None = None) -> dict[str, Any]:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", **(headers or {})},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", "replace")
        try:
            message = json.loads(body).get("error", {}).get("message", body)
        except json.JSONDecodeError:
            message = body
        raise RuntimeError(str(message)) from error
    return json.loads(body)


def create_promo_batch(
    plan_id: str,
    count: int,
    max_uses: int = 1,
    expires_days: int = 30,
    premium_days: int | None = None,
) -> list[str]:
    if not DEVAI_ADMIN_TOKEN:
        raise RuntimeError("DEVAI_ADMIN_TOKEN не задан")
    plan = PLANS.get(plan_id)
    if plan is None:
        raise RuntimeError("Неизвестный тариф")
    response = http_json(
        DEVAI_API_BASE + "/admin/promo",
        {
            "plan": plan.id,
            "count": max(1, min(int(count), 100)),
            "max_uses": max(1, min(int(max_uses), 1000)),
            "days": plan.days_value if premium_days is None else max(0, min(int(premium_days), 3650)),
            "expires_days": max(0, min(int(expires_days), 3650)),
        },
        {"X-Admin-Token": DEVAI_ADMIN_TOKEN},
    )
    codes = response.get("codes", [])
    if not isinstance(codes, list):
        raise RuntimeError("Сервер вернул некорректный ответ")
    return [str(code) for code in codes]


def issue_promo(plan_id: str) -> str:
    codes = create_promo_batch(plan_id, 1, max_uses=1, expires_days=30)
    return codes[0] if codes else ""


def create_api_key(plan_id: str, telegram_id: int) -> dict[str, Any]:
    if not DEVAI_ADMIN_TOKEN:
        raise RuntimeError("DEVAI_ADMIN_TOKEN не задан")
    plan = API_PLANS.get(plan_id)
    if plan is None:
        raise RuntimeError("Неизвестный тариф API")
    return http_json(
        DEVAI_API_BASE + "/admin/keys",
        {
            "name": f"API · Telegram {telegram_id}",
            "plan": plan.id.removeprefix("api_"),
        },
        {"X-Admin-Token": DEVAI_ADMIN_TOKEN},
    )


def send(chat_id: int, text: str, keyboard: dict[str, Any] | None = None) -> Any:
    payload: dict[str, Any] = {
        "chat_id": chat_id,
        "text": text,
        "disable_web_page_preview": True,
    }
    if keyboard:
        payload["reply_markup"] = keyboard
    return api("sendMessage", payload)


def inline(rows: list[list[dict[str, str]]]) -> dict[str, Any]:
    return {"inline_keyboard": rows}


def main_menu() -> dict[str, Any]:
    rows: list[list[dict[str, Any]]] = [
        [{"text": "Бесплатный промокод", "callback_data": "free_promo"}],
        [
            {"text": "Купить Premium", "callback_data": "buy"},
            {"text": "Купить API-ключ", "callback_data": "api_buy"},
        ],
        [
            {"text": "Партнёрка", "callback_data": "partner"},
            {"text": "Баланс", "callback_data": "balance"},
        ],
    ]
    if BOT_USERNAME:
        url = f"{MINI_APP_URL}?bot={BOT_USERNAME}"
        rows.append([
            {"text": "Mini App", "web_app": {"url": url}},
            {"text": "В браузере", "url": url},
        ])
    rows.append([{"text": "Помощь", "callback_data": "help"}])
    return inline(rows)


def back_row() -> list[dict[str, str]]:
    return [{"text": "Назад", "callback_data": "back"}]


def is_admin(telegram_id: int) -> bool:
    return telegram_id in ADMIN_IDS


def welcome_text(first_name: str, referred: bool) -> str:
    name = f", {first_name}" if first_name else ""
    suffix = "\n\nТы пришёл по партнёрской ссылке. Партнёр получит комиссию после покупки." if referred else ""
    return (
        f"Привет{name}.\n\n"
        "Это DevAI Premium — доступ к темам, локальным моделям, поиску, плагинам и MCP.\n"
        "Здесь же можно купить готовый API-ключ DevAI Core.\n"
        "Оплата проходит в Telegram Stars. Без карт и автосписаний."
        f"{suffix}"
    )


def handle_start(chat_id: int, user: dict[str, Any], payload: str) -> None:
    ref_code = payload[4:] if payload.startswith("ref_") else None
    row = ensure_user(int(user["id"]), user.get("username", ""), user.get("first_name", ""), ref_code)
    send(chat_id, welcome_text(user.get("first_name", ""), bool(row["referred_by"])), main_menu())
    if payload.startswith("buy_"):
        handle_plan(chat_id, int(user["id"]), payload[4:])
    elif payload == "api":
        handle_api_buy(chat_id)
    elif payload == "partner":
        handle_partner(chat_id, int(user["id"]))


def handle_buy(chat_id: int) -> None:
    rows = [
        [{"text": f"{plan.title} — {fmt_stars(plan.stars)} ⭐ / {plan.days}", "callback_data": f"plan:{plan.id}"}]
        for plan in PLANS.values()
    ]
    rows.append(back_row())
    send(chat_id, "Выбери тариф Premium. Оплата пройдёт в Telegram Stars.", inline(rows))


def handle_api_buy(chat_id: int) -> None:
    rows = [
        [{"text": f"{plan.title} — {fmt_stars(plan.stars)} ⭐ / {plan.days}", "callback_data": f"apiplan:{plan.id}"}]
        for plan in API_PLANS.values()
    ]
    rows.append(back_row())
    send(
        chat_id,
        "API-ключ DevAI Core\n\n"
        "После оплаты бот пришлёт готовый ключ прямо в этот чат. "
        "Лимит Premium: 100 000 токенов в месяц и 60 запросов в минуту.",
        inline(rows),
    )


def handle_plan(chat_id: int, user_id: int, plan_id: str) -> None:
    plan = ALL_PLANS.get(plan_id)
    if plan is None:
        send(chat_id, "Такого тарифа нет.", main_menu())
        return
    is_api = plan.id.startswith("api_")
    order_id = create_order(user_id, plan.id)
    api("sendInvoice", {
        "chat_id": chat_id,
        "title": f"{'DevAI API' if is_api else 'DevAI Premium'} — {plan.title}",
        "description": (
            f"API-ключ DevAI Core на {plan.days}. После оплаты пришлю ключ в чат."
            if is_api
            else f"Доступ к Premium на {plan.days}. После оплаты бот пришлёт код активации."
        ),
        "payload": f"order:{order_id}",
        "provider_token": "",
        "currency": "XTR",
        "prices": [{"label": plan.title, "amount": plan.stars}],
    })


def handle_pre_checkout(query: dict[str, Any]) -> None:
    payload = str(query.get("invoice_payload", ""))
    order_id = payload[6:] if payload.startswith("order:") else ""
    order = get_order(order_id) if order_id else None
    ok = bool(order and order["status"] == "pending" and query.get("currency") == "XTR")
    api("answerPreCheckoutQuery", {
        "pre_checkout_query_id": query["id"],
        "ok": ok,
        **({} if ok else {"error_message": "Заказ не найден или уже оплачен. Создай новый заказ."}),
    })


def handle_successful_payment(chat_id: int, payment: dict[str, Any]) -> None:
    payload = str(payment.get("invoice_payload", ""))
    order_id = payload[6:] if payload.startswith("order:") else ""
    if not order_id:
        return
    order = mark_order_paid(order_id, str(payment.get("telegram_payment_charge_id", "")))
    if order is None:
        send(chat_id, "Платёж получен, но заказ уже был обработан. Если код не пришёл — напиши администратору.")
        return

    plan = ALL_PLANS.get(order["plan"])
    is_api = bool(plan and plan.id.startswith("api_"))
    delivery = ""
    api_meta: dict[str, Any] = {}
    if plan and DEVAI_ADMIN_TOKEN:
        try:
            if is_api:
                api_meta = create_api_key(plan.id, int(order["buyer_id"]))
                delivery = str(api_meta.get("key", ""))
            else:
                delivery = issue_promo(plan.id)
        except Exception:
            log.exception("Не удалось выдать доступ для заказа %s", order["id"])
    if not delivery:
        send(
            chat_id,
            "Оплата прошла, но доступ не создан.\n\n"
            "Напиши администратору — он выдаст его вручную.",
            main_menu(),
        )
        for admin_id in ADMIN_IDS:
            try:
                send(
                    admin_id,
                    "Оплата без доступа\n\n"
                    f"Заказ: {order['id']}\n"
                    f"Покупатель: {order['buyer_id']}\n"
                    f"Тариф: {plan.title if plan else order['plan']}",
                )
            except Exception:
                log.exception("Не удалось уведомить админа %s", admin_id)
        return
    if is_api:
        limit = int(api_meta.get("token_limit") or 0)
        rpm = int(api_meta.get("rpm") or 60)
        send(
            chat_id,
            "Оплата прошла. Спасибо!\n\n"
            f"Тариф: {plan.title}\n"
            f"API-ключ: {delivery}\n\n"
            f"Base URL: {DEVAI_API_BASE}/v1\n"
            f"Лимит: {fmt_stars(limit)} токенов/мес · {rpm} запросов/мин.\n\n"
            "Сохрани ключ: сервер хранит только его хеш.",
            main_menu(),
        )
    else:
        send(
            chat_id,
            "Оплата прошла. Спасибо!\n\n"
            f"Тариф: {plan.title if plan else order['plan']}\n"
            f"Код активации: {delivery}\n\n"
            "Открой DevAI Studio → Premium → Активация и вставь код.",
            main_menu(),
        )

    if order["partner_id"] and order["commission"]:
        try:
            send(
                int(order["partner_id"]),
                "Партнёрское начисление\n\n"
                f"Покупка: {plan.title if plan else order['plan']}\n"
                f"Твоя комиссия: {fmt_stars(order['commission'])} ⭐\n\n"
                "Баланс можно вывести в разделе «Партнёрка».",
            )
        except Exception:
            log.exception("Не удалось уведомить партнёра %s", order["partner_id"])

    for admin_id in ADMIN_IDS:
        try:
            send(
                admin_id,
                "Новая оплата\n\n"
                f"Заказ: {order['id']}\n"
                f"Тариф: {plan.title if plan else order['plan']}\n"
                f"Сумма: {fmt_stars(order['amount'])} ⭐\n"
                f"Комиссия партнёра: {fmt_stars(order['commission'])} ⭐",
            )
        except Exception:
            log.exception("Не удалось уведомить админа %s", admin_id)


def handle_free_promo(chat_id: int, user_id: int) -> None:
    if not DEVAI_ADMIN_TOKEN:
        send(chat_id, "Бесплатные промокоды временно недоступны. Попробуй позже.", main_menu())
        return
    claim_id, next_at = reserve_free_promo(user_id)
    if claim_id is None:
        when = fmt_remaining(next_at) if next_at else f"{FREE_PROMO_COOLDOWN_DAYS} дн."
        send(
            chat_id,
            "Бесплатный промокод уже получен.\n\n"
            f"Следующий можно взять через {when}.\n"
            f"Один бесплатный код раз в {FREE_PROMO_COOLDOWN_DAYS} дн.",
            main_menu(),
        )
        return
    try:
        codes = create_promo_batch(
            "week",
            1,
            max_uses=1,
            expires_days=FREE_PROMO_CODE_TTL_DAYS,
            premium_days=FREE_PROMO_DAYS,
        )
        code = codes[0] if codes else ""
        if not code:
            raise RuntimeError("Сервер не вернул код")
    except Exception as error:
        cancel_free_promo(claim_id)
        log.exception("Не удалось выдать бесплатный промокод пользователю %s", user_id)
        send(chat_id, f"Не удалось выдать промокод: {error}\nПопробуй позже.", main_menu())
        return
    finish_free_promo(claim_id, code)
    send(
        chat_id,
        "Бесплатный промокод\n\n"
        f"Код: {code}\n"
        f"Premium: {FREE_PROMO_DAYS} дн.\n"
        f"Код одноразовый и действует {FREE_PROMO_CODE_TTL_DAYS} дн.\n\n"
        "Открой DevAI Studio → Premium → Активация и вставь код.\n\n"
        f"Следующий бесплатный код — через {FREE_PROMO_COOLDOWN_DAYS} дн.",
        main_menu(),
    )


def handle_partner(chat_id: int, user_id: int) -> None:
    partner = ensure_partner(user_id)
    link = partner_link(partner["code"])
    if not link:
        send(
            chat_id,
            "У бота пока нет username. Задай его через @BotFather и перезапусти бота.",
            inline([back_row()]),
        )
        return
    send(
        chat_id,
        "Партнёрская программа\n\n"
        f"Твоя ссылка:\n{link}\n\n"
        f"Код: {partner['code']}\n"
        f"Переходов: {partner['starts']}\n"
        f"Продаж: {partner['sales']}\n"
        f"Заработано: {fmt_stars(partner['earned'])} ⭐\n"
        f"На балансе: {fmt_stars(partner['balance'])} ⭐\n\n"
        f"Комиссия — {round(COMMISSION_RATE * 100)}% с каждой покупки. "
        f"Минимальный вывод — {fmt_stars(MIN_PAYOUT)} ⭐.",
        inline([[{"text": "Вывести всё", "callback_data": "payout_all"}], back_row()]),
    )


def handle_balance(chat_id: int, user_id: int) -> None:
    partner = ensure_partner(user_id)
    rows = [[{"text": "Вывести всё", "callback_data": "payout_all"}], back_row()] if partner["balance"] >= MIN_PAYOUT else [back_row()]
    send(
        chat_id,
        "Твой баланс\n\n"
        f"Доступно: {fmt_stars(partner['balance'])} ⭐\n"
        f"Всего заработано: {fmt_stars(partner['earned'])} ⭐\n"
        f"Выплачено: {fmt_stars(partner['paid'])} ⭐\n"
        f"Минимум для вывода: {fmt_stars(MIN_PAYOUT)} ⭐",
        inline(rows),
    )


def notify_admins_about_payout(payout_id: str, user_id: int, amount: int) -> None:
    for admin_id in ADMIN_IDS:
        try:
            send(
                admin_id,
                "Запрос на вывод Stars\n\n"
                f"Заявка: {payout_id}\n"
                f"Партнёр: {user_id}\n"
                f"Сумма: {fmt_stars(amount)} ⭐\n\n"
                f"Выплати вручную и отметь: /paid {payout_id}\n"
                f"Или отклони: /reject {payout_id}",
            )
        except Exception:
            log.exception("Не удалось уведомить админа %s", admin_id)


def handle_payout_all(chat_id: int, user_id: int) -> None:
    partner = ensure_partner(user_id)
    if partner["balance"] < MIN_PAYOUT:
        send(chat_id, f"Минимальная сумма вывода — {fmt_stars(MIN_PAYOUT)} ⭐.", inline([back_row()]))
        return
    payout_id, error = request_payout(user_id, int(partner["balance"]))
    if error:
        send(chat_id, error, inline([back_row()]))
        return
    notify_admins_about_payout(payout_id, user_id, int(partner["balance"]))
    send(
        chat_id,
        f"Заявка {payout_id} создана на {fmt_stars(partner['balance'])} ⭐.\n"
        "Админ выплатит Stars вручную и отметит заявку.",
        inline([back_row()]),
    )


def handle_payout_command(chat_id: int, user_id: int, args: list[str]) -> None:
    if not args or not args[0].isdigit():
        send(chat_id, f"Напиши сумму: /payout 100. Минимум — {fmt_stars(MIN_PAYOUT)} ⭐.")
        return
    amount = int(args[0])
    payout_id, error = request_payout(user_id, amount)
    if error:
        send(chat_id, error)
        return
    notify_admins_about_payout(payout_id, user_id, amount)
    send(chat_id, f"Заявка {payout_id} создана на {fmt_stars(amount)} ⭐.")


def handle_admin(chat_id: int, user_id: int) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    stats = admin_stats()
    text = (
        "Админка\n\n"
        f"Пользователей: {stats['users']}\n"
        f"Партнёров: {stats['partners']}\n"
        f"Оплаченных заказов: {stats['paid_orders']}\n"
        f"Оборот: {fmt_stars(stats['paid_stars'])} ⭐\n"
        f"Заявок на вывод: {stats['pending_payouts']} на {fmt_stars(stats['pending_stars'])} ⭐\n\n"
        "Команды: /promo, /payouts, /orders, /paid PO-..., /reject PO-..., /botstars"
    )
    send(chat_id, text)


def handle_payouts(chat_id: int, user_id: int) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    rows = pending_payouts()
    if not rows:
        send(chat_id, "Заявок на вывод нет.")
        return
    lines = ["Очередь выплат:", ""]
    for row in rows:
        lines.append(f"{row['id']} · {row['partner_id']} · {fmt_stars(row['amount'])} ⭐")
    lines.append("")
    lines.append("Выплати вручную, затем: /paid <id>")
    send(chat_id, "\n".join(lines))


def handle_orders(chat_id: int, user_id: int) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    rows = recent_orders()
    if not rows:
        send(chat_id, "Заказов пока нет.")
        return
    lines = ["Последние заказы:", ""]
    for row in rows:
        lines.append(f"{row['id']} · {row['plan']} · {fmt_stars(row['amount'])} ⭐ · {row['status']}")
    send(chat_id, "\n".join(lines))


def handle_bot_stars(chat_id: int, user_id: int) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    try:
        balance = api("getMyStarBalance")
        send(chat_id, f"Баланс бота: {fmt_stars(int(balance.get('amount', 0)))} ⭐")
    except Exception as error:
        send(chat_id, f"Не удалось получить баланс: {error}")


def handle_paid(chat_id: int, user_id: int, args: list[str]) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    if not args:
        send(chat_id, "Укажи заявку: /paid PO-...")
        return
    payout = mark_payout_paid(args[0])
    if payout is None:
        send(chat_id, "Заявка не найдена или уже обработана.")
        return
    send(chat_id, f"Заявка {payout['id']} отмечена как выплаченная.")
    try:
        send(int(payout["partner_id"]), f"Выплата {fmt_stars(payout['amount'])} ⭐ отправлена. Спасибо за работу!")
    except Exception:
        log.exception("Не удалось уведомить партнёра о выплате")


def handle_reject(chat_id: int, user_id: int, args: list[str]) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    if not args:
        send(chat_id, "Укажи заявку: /reject PO-...")
        return
    payout = reject_payout(args[0])
    if payout is None:
        send(chat_id, "Заявка не найдена или уже обработана.")
        return
    send(chat_id, f"Заявка {payout['id']} отклонена, звёзды возвращены на баланс.")
    try:
        send(int(payout["partner_id"]), f"Заявка {payout['id']} отклонена. {fmt_stars(payout['amount'])} ⭐ вернулись на баланс.")
    except Exception:
        log.exception("Не удалось уведомить партнёра об отклонении")


def handle_promo(chat_id: int, user_id: int, args: list[str]) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    if not args or args[0].lower() in {"help", "?"}:
        send(
            chat_id,
            "Генератор промокодов\n\n"
            "/promo <тариф> <количество> [использований] [дней_жизни]\n"
            "Тарифы: week, month, year, lifetime\n"
            "Пример: /promo month 5 1 30\n\n"
            "Коды одноразовые по умолчанию и работают в DevAI через сервер.",
        )
        return
    plan_id = args[0].lower()
    if plan_id not in PLANS:
        send(chat_id, "Тарифы: week, month, year, lifetime.")
        return
    try:
        count = int(args[1]) if len(args) > 1 else 1
        max_uses = int(args[2]) if len(args) > 2 else 1
        expires_days = int(args[3]) if len(args) > 3 else 30
    except ValueError:
        send(chat_id, "Количество, использования и дни должны быть числами.")
        return
    count = max(1, min(count, 50))
    max_uses = max(1, min(max_uses, 1000))
    expires_days = max(0, min(expires_days, 3650))
    try:
        codes = create_promo_batch(plan_id, count, max_uses, expires_days)
    except Exception as error:
        send(chat_id, f"Не удалось создать коды: {error}")
        return
    plan = PLANS[plan_id]
    lines = [
        f"Промокоды · {plan.title}",
        f"Использований на код: {max_uses} · срок: {expires_days or 'бессрочно'} дн.",
        "",
    ]
    lines.extend(codes)
    send(chat_id, "\n".join(lines))


def handle_trial(chat_id: int, user_id: int, args: list[str]) -> None:
    if not is_admin(user_id):
        send(chat_id, "Нет доступа.")
        return
    if not args or args[0].lower() in {"help", "?"}:
        send(
            chat_id,
            "Бесплатные временные промокоды\n\n"
            "/trial <дней> [количество] [дней_жизни]\n"
            "Пример: /trial 2 10 30 — 10 кодов на 2 дня Premium, сами коды действуют 30 дней.\n\n"
            "Коды одноразовые, бесплатные и активируются в DevAI.",
        )
        return
    try:
        days = int(args[0])
        count = int(args[1]) if len(args) > 1 else 1
        expires_days = int(args[2]) if len(args) > 2 else 30
    except ValueError:
        send(chat_id, "Дни, количество и срок должны быть числами.")
        return
    days = max(1, min(days, 30))
    count = max(1, min(count, 50))
    expires_days = max(0, min(expires_days, 3650))
    try:
        codes = create_promo_batch("week", count, max_uses=1, expires_days=expires_days, premium_days=days)
    except Exception as error:
        send(chat_id, f"Не удалось создать коды: {error}")
        return
    lines = [
        f"Бесплатные пробные коды · Premium на {days} дн.",
        f"Кодов: {len(codes)} · каждый код действует {expires_days or 'бессрочно'} дн.",
        "",
    ]
    lines.extend(codes)
    send(chat_id, "\n".join(lines))


def handle_mini(chat_id: int) -> None:
    if not BOT_USERNAME:
        send(chat_id, "Сначала задай username бота через @BotFather.")
        return
    url = f"{MINI_APP_URL}?bot={BOT_USERNAME}"
    send(chat_id, "Mini App с тарифами и партнёркой:", inline([[
        {"text": "Открыть Mini App", "web_app": {"url": url}},
        {"text": "В браузере", "url": url},
    ]]))


def handle_help(chat_id: int) -> None:
    send(
        chat_id,
        "Как это работает\n\n"
        "1. Выбери тариф и оплати Telegram Stars.\n"
        "2. Бот пришлёт код активации.\n"
        "3. Вставь код в DevAI Studio → Premium.\n\n"
        "API-ключ: /api или кнопка «Купить API-ключ». После оплаты ключ придёт сразу в чат.\n\n"
        f"Бесплатный промокод: кнопка в меню или /free. Один код раз в {FREE_PROMO_COOLDOWN_DAYS} дн., Premium на {FREE_PROMO_DAYS} дн.\n\n"
        "Mini App: кнопка «Mini App» в меню или команда /mini.\n\n"
        "Партнёрка: открой «Партнёрка», получи ссылку и делись ею. "
        f"Комиссия — {round(COMMISSION_RATE * 100)}% с покупки. Вывод — от {fmt_stars(MIN_PAYOUT)} ⭐ вручную.\n\n"
        f"Подробнее: {PUBLIC_URL}",
        inline([back_row()]),
    )


def handle_message(message: dict[str, Any]) -> None:
    chat = message.get("chat", {})
    user = message.get("from", {})
    chat_id = int(chat.get("id", 0))
    user_id = int(user.get("id", 0))
    text = str(message.get("text", "")).strip()
    if not chat_id or not user_id:
        return

    if text.startswith("/start"):
        parts = text.split(maxsplit=1)
        payload = parts[1] if len(parts) > 1 else ""
        handle_start(chat_id, user, payload)
        return

    if text.startswith("/buy"):
        handle_buy(chat_id)
    elif text.startswith("/api"):
        handle_api_buy(chat_id)
    elif text.startswith("/free"):
        handle_free_promo(chat_id, user_id)
    elif text.startswith("/trial"):
        handle_trial(chat_id, user_id, text.split()[1:])
    elif text.startswith("/partner"):
        handle_partner(chat_id, user_id)
    elif text.startswith("/balance"):
        handle_balance(chat_id, user_id)
    elif text.startswith("/payouts"):
        handle_payouts(chat_id, user_id)
    elif text.startswith("/payout"):
        handle_payout_command(chat_id, user_id, text.split()[1:])
    elif text.startswith("/admin"):
        handle_admin(chat_id, user_id)
    elif text.startswith("/orders"):
        handle_orders(chat_id, user_id)
    elif text.startswith("/promo"):
        handle_promo(chat_id, user_id, text.split()[1:])
    elif text.startswith("/mini"):
        handle_mini(chat_id)
    elif text.startswith("/botstars"):
        handle_bot_stars(chat_id, user_id)
    elif text.startswith("/paid"):
        handle_paid(chat_id, user_id, text.split()[1:])
    elif text.startswith("/reject"):
        handle_reject(chat_id, user_id, text.split()[1:])
    elif text.startswith("/help"):
        handle_help(chat_id)
    elif text.startswith("/id"):
        send(chat_id, f"Твой Telegram ID: {user_id}")
    else:
        send(chat_id, "Выбери действие в меню.", main_menu())


def handle_callback(callback: dict[str, Any]) -> None:
    callback_id = callback.get("id")
    user = callback.get("from", {})
    message = callback.get("message", {})
    chat_id = int(message.get("chat", {}).get("id", 0))
    user_id = int(user.get("id", 0))
    data = str(callback.get("data", ""))
    if callback_id:
        try:
            api("answerCallbackQuery", {"callback_query_id": callback_id})
        except Exception:
            log.exception("answerCallbackQuery failed")

    if not chat_id or not user_id:
        return
    if data == "buy":
        handle_buy(chat_id)
    elif data == "api_buy":
        handle_api_buy(chat_id)
    elif data == "free_promo":
        handle_free_promo(chat_id, user_id)
    elif data.startswith("plan:"):
        handle_plan(chat_id, user_id, data.split(":", 1)[1])
    elif data.startswith("apiplan:"):
        handle_plan(chat_id, user_id, data.split(":", 1)[1])
    elif data == "partner":
        handle_partner(chat_id, user_id)
    elif data == "balance":
        handle_balance(chat_id, user_id)
    elif data == "payout_all":
        handle_payout_all(chat_id, user_id)
    elif data == "help":
        handle_help(chat_id)
    else:
        send(chat_id, "Главное меню.", main_menu())


def handle_update(update: dict[str, Any]) -> None:
    if "pre_checkout_query" in update:
        handle_pre_checkout(update["pre_checkout_query"])
        return

    message = update.get("message")
    if message:
        if "successful_payment" in message:
            handle_successful_payment(int(message["chat"]["id"]), message["successful_payment"])
            return
        handle_message(message)
        return

    if "callback_query" in update:
        handle_callback(update["callback_query"])


def configure_bot() -> None:
    try:
        api("setMyCommands", {
            "commands": [
                {"command": "start", "description": "Главное меню"},
                {"command": "buy", "description": "Купить Premium"},
                {"command": "api", "description": "Купить API-ключ"},
                {"command": "free", "description": "Бесплатный промокод"},
                {"command": "partner", "description": "Партнёрская ссылка и баланс"},
                {"command": "balance", "description": "Баланс Stars"},
                {"command": "payout", "description": "Запросить вывод Stars"},
                {"command": "mini", "description": "Открыть Mini App"},
                {"command": "id", "description": "Показать Telegram ID"},
                {"command": "help", "description": "Как это работает"},
            ]
        })
    except Exception:
        log.exception("setMyCommands failed")

    if not BOT_USERNAME or not MINI_APP_URL:
        return
    try:
        api("setChatMenuButton", {
            "menu_button": {
                "type": "web_app",
                "text": "Premium",
                "web_app": {"url": f"{MINI_APP_URL}?bot={BOT_USERNAME}"},
            }
        })
        log.info("Mini App menu button set")
    except Exception:
        log.exception("setChatMenuButton failed")


def main() -> int:
    global BOT_USERNAME
    if not TOKEN:
        print(
            "TELEGRAM_BOT_TOKEN не задан.\n"
            "Создай бота у @BotFather и запусти:\n"
            "  export TELEGRAM_BOT_TOKEN='...'\n"
            "  export TELEGRAM_ADMIN_IDS='твой_telegram_id'\n"
            "  python3 server/stars_bot.py",
            file=sys.stderr,
        )
        return 1

    init_db()
    try:
        me = api("getMe")
        BOT_USERNAME = str(me.get("username", "")).lstrip("@")
        log.info("Bot @%s started", BOT_USERNAME or "unknown")
        configure_bot()
    except Exception:
        log.exception("Не удалось получить данные бота. Проверь токен.")
        return 1

    if not ADMIN_IDS:
        log.warning("TELEGRAM_ADMIN_IDS пуст: админ-команды и уведомления о выплатах отключены")
    if not DEVAI_ADMIN_TOKEN:
        log.warning("DEVAI_ADMIN_TOKEN пуст: создание промокодов отключено")

    offset = 0
    log.info("Polling started")
    while True:
        try:
            updates = api("getUpdates", {
                "offset": offset,
                "timeout": 25,
                "allowed_updates": ["message", "callback_query", "pre_checkout_query"],
            })
            for update in updates:
                offset = max(offset, int(update["update_id"]) + 1)
                try:
                    handle_update(update)
                except Exception:
                    log.exception("Update failed: %s", update.get("update_id"))
        except KeyboardInterrupt:
            log.info("Stopped")
            return 0
        except Exception:
            log.exception("Polling error; retrying in 3s")
            time.sleep(3)


if __name__ == "__main__":
    raise SystemExit(main())
