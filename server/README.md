# DevAI Core API v0.5

OpenAI-compatible HTTP API for DevAI Core. It runs a local Qwen2.5 0.5B GGUF
model, learns from documentation and public pages without user involvement,
opens links shared in chat (including `t.me`), and scans links/files with a
dependency-free heuristic antivirus.

## Quick start

```bash
python3 -m pip install -r server/requirements.txt
mkdir -p server/models
curl -L -o server/models/qwen2.5-0.5b-instruct-q4_k_m.gguf \
  https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf
DEVAI_MODEL_PATH="$PWD/server/models/qwen2.5-0.5b-instruct-q4_k_m.gguf" \
DEVAI_PORT=12001 python3 server/devai_server.py
```

The server creates `server/data/admin_token` on first start. Keep that file
private: it can create and revoke API keys.

## Client settings

- Base URL: `https://<host>/v1`
- API key: `devai-sk-...`
- Model: `devai-core-0.2`

## Endpoints

- `GET /health` — model status, no key required.
- `GET /v1/models` — model list, key required.
- `POST /v1/chat/completions` — OpenAI-compatible chat, key required.
  Supports `stream: true` (SSE).
- `POST /v1/keys/challenge` — one-time proof-of-work challenge for public signup.
- `POST /v1/keys/register` — create a key from the public website, rate-limited.
- `POST /admin/keys` — create a key, admin token required.
- `GET /admin/keys` — list key metadata, admin token required.
- `DELETE /admin/keys/<id>` — revoke a key, admin token required.
- `POST /admin/keys/<id>/rotate` — replace a key, admin token required.
- `GET /v1/auth/sites` — websites sharing one account.
- `GET /v1/auth/check?nickname=` — nickname availability.
- `POST /v1/auth/register` — `{nickname, password, site}`; creates memberships for all sites.
- `POST /v1/auth/login` — `{nickname, password, site}`; returns a bearer token.
- `GET /v1/auth/me?site=` — current account and its site memberships.
- `POST /v1/auth/logout` — revoke the bearer token.
- `PUT /v1/auth/profile` — change password; nickname changes are rejected.
- `POST /admin/accounts/owner` — create the owner account, admin token required.
- `POST /v1/scan` — check `{url}`, `{text, filename}` or `{content_base64, filename}`; returns verdict, score, SHA-256 and findings.
- `GET /admin/learning` — learning counters, queue and recent items, admin token required.
- `GET /admin/learning/feed?after=<id>&limit=20` — new learned items for the channel publisher, admin token required.
- `GET /admin/learning/featured?limit=5` — best project-related items for a digest, admin token required.
- `POST /admin/learning/run` — run one crawl/learning cycle now, admin token required.
- `POST /admin/learning/url` — open a URL and queue its links for learning, admin token required.
- `GET /admin/scans?limit=30` — recent scan results, admin token required.

## Create a key

```bash
ADMIN=$(cat server/data/admin_token)
curl -s -X POST http://127.0.0.1:12001/admin/keys \
  -H "X-Admin-Token: $ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"name":"android","rpm":60}'
```

The response contains `key` once. The database stores only its SHA-256 hash.

Paid API keys use a plan and get the Premium limits for its duration:

```bash
curl -s -X POST http://127.0.0.1:12001/admin/keys \
  -H "X-Admin-Token: $ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"name":"telegram-123","plan":"month"}'
```

Plans: `week`, `month`, `year`, `lifetime`. The Stars bot uses this endpoint
after a successful payment and sends the returned key to the buyer.

## Shared accounts

`docs/auth.js` adds one account widget to every DevAI site. A registration
creates the account and a membership for every site in `SITES`; other sites
restore the same bearer token from `localStorage` and call `/v1/auth/me`.
Nicknames are unique (case-insensitive) and immutable: `PUT /v1/auth/profile`
rejects any nickname change. Passwords are scrypt-hashed and must reach at
least the "Средний" level (8+ characters and multiple character classes).

## Public website signup

The static site in `docs/` creates keys without exposing the admin token:

1. `POST /v1/keys/challenge` returns a random challenge and difficulty.
2. The browser finds a nonce whose `sha256("<challenge>:<nonce>")` starts with
   `0000` (four hex zeroes).
3. `POST /v1/keys/register` verifies the one-time challenge, binds it to the
   client IP, and returns a new key.

Anti-bot controls: proof-of-work, a 1.2-second minimum challenge age, a hidden
honeypot field, a cap of 8 open challenges per IP, and 3 keys per IP per day
(300 per day globally). Challenges expire after 5 minutes and are consumed on
first use. The client IP is hashed with the admin token before it is stored.

## Autonomous learning

A background thread runs every 90 seconds by default. Each cycle:

1. seeds the queue with the project sites, GitHub repository and `t.me` bot;
2. indexes Markdown documentation from the repository;
3. fetches up to 3 queued pages, stores their text, and queues safe links
   one or two levels deep;
4. saves searchable chunks in SQLite and retrieves the best matches for each
   chat request.

Retrieval-augmented context is injected into the system prompt. This is
continual learning in the product sense (new knowledge without retraining),
not a change of model weights. Malicious pages are not learned. Website text is
marked as untrusted data in the prompt.

Useful environment variables:

- `DEVAI_AUTOL_EARN=0` — disable the background learner.
- `DEVAI_AUTOL_EARN_INTERVAL=90` — seconds between cycles.
- `DEVAI_AUTOL_EARN_BATCH=3` — pages per cycle.
- `DEVAI_AUTOL_EARN_DEPTH=2` — crawl depth.
- `DEVAI_AUTOL_EARN_DOMAIN_LIMIT=25` — pages per domain.
- `DEVAI_AUTOL_EARN_QUEUE=500` — queue cap.

## Antivirus

`server/devai_av.py` is a stdlib-only heuristic scanner. It checks:

- PE/ELF executables, packed binaries and high entropy;
- dangerous extensions, double extensions and RTL-override filenames;
- ZIP/APK/JAR archives, nested executables, zip-slip paths and zip bombs;
- Office macros (`vbaProject.bin`, `AutoOpen`, `Document_Open`);
- PDF JavaScript, `OpenAction`, `Launch` and embedded files;
- PowerShell encoded commands, download-and-execute, reverse shells,
  credential dumping, ransomware notes, persistence and keyloggers;
- HTML password forms posting to another domain, hidden iframes and obfuscated JS;
- the EICAR test file.

The scanner never executes uploads. A verdict is `clean`, `suspicious`,
`malicious` or `unknown`, with a 0–100 score. It is a first line of defence,
not a replacement for ClamAV or VirusTotal.

### Links

`server/devai_web.py` normalises `http(s)://`, `www.` and `t.me/` links, blocks
private/loopback hosts, follows at most four redirects, re-checks every redirect,
respects `robots.txt`, limits downloads to 1.5 MB and extracts readable text,
titles, descriptions and links. Telegram launch pages are fetched through the
public preview page because chat contents require the Telegram app.

## Telegram channel bot

`server/devai_ai_bot.py` runs @devaiAI_bot and publishes to `@devaiAI`.

It can:

- accept a DevAI API key with `/key devai-sk-...`, delete the message with
  the key, and answer `/ai` prompts (or plain text) through the API;
- publish a formatted digest of new project knowledge in Russian, with a short
  retelling for each item and a source link; English-only items are skipped;
- publish a first-person persona post on its own schedule (about every 4 hours
  with jitter): what it likes, what it is learning, how it works and where to
  find it;
- reply to comments in the channel discussion group;
- read uploaded books and documents (`txt`, `md`, `html`, `epub`, `fb2`,
  `docx`, `pdf`, `rtf`) and store them in the learning database.

Required secret files (both ignored by git, mode 600):

- `server/data/devai_ai_bot_token` — token of @devaiAI_bot;
- `server/data/devai_ai_bot.env` — `TELEGRAM_COMMENTS_CHAT`, admin ids and
  intervals.

Add @devaiAI_bot to the linked discussion group as an administrator so it can
read comments. Send a book as a file with the optional caption `/book Название`.
Admins can also use `/read <url>` to learn one public page. The bot never claims to be human: persona posts state that it is
an AI character. Comment replies are rate-limited per user and per minute.

## Limits

- 60 requests/minute per key by default (`rpm` can be set up to 600).
- 1024 output tokens per request.
- One generation at a time per process; this is intentional for CPU inference.
- Public signup: 3 keys per IP per day, 300 keys per day globally.
- Scanned file: up to 8 MB (12 MB JSON request body).
- Crawler: 1.5 MB per page, 3–20 pages per cycle, 25 pages per domain.
