# DevAI Core API v0.1

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

## Limits

- 60 requests/minute per key by default (`rpm` can be set up to 600).
- 1024 output tokens per request.
- One generation at a time per process; this is intentional for CPU inference.
