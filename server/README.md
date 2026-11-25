# DevAI Core API v0.3

OpenAI-compatible HTTP API for DevAI Core. The first version runs a local
Qwen2.5 0.5B GGUF model, so prompts do not leave the machine.

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
- Model: `devai-core-0.1`

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

## Limits

- 60 requests/minute per key by default (`rpm` can be set up to 600).
- 1024 output tokens per request.
- One generation at a time per process; this is intentional for CPU inference.
- Public signup: 3 keys per IP per day, 300 keys per day globally.
