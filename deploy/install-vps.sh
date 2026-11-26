#!/usr/bin/env bash
# One-time installer for a Debian/Ubuntu VPS.
# Usage:
#   sudo TELEGRAM_BOT_TOKEN='123:ABC' bash deploy/install-vps.sh
# Optional:
#   sudo TELEGRAM_ADMIN_IDS='123456' TELEGRAM_COMMENTS_CHAT='-100...' bash deploy/install-vps.sh
set -euo pipefail

APP_DIR=/opt/devai/app
DATA_DIR="$APP_DIR/server/data"
MODEL_DIR="$APP_DIR/server/models"
MODEL="$MODEL_DIR/google_gemma-3-4b-it-Q4_K_M.gguf"
REPO="https://github.com/Karishka-sr/devai-studio.git"

if [ "$(id -u)" -ne 0 ]; then
  echo "Run as root: sudo TELEGRAM_BOT_TOKEN='...' bash deploy/install-vps.sh" >&2
  exit 1
fi

TOKEN="${TELEGRAM_BOT_TOKEN:-}"
if [ -z "$TOKEN" ] && [ -f /opt/devai/telegram_bot_token ]; then
  TOKEN="$(cat /opt/devai/telegram_bot_token)"
fi
if [ -z "$TOKEN" ]; then
  echo "Set TELEGRAM_BOT_TOKEN or put the token in /opt/devai/telegram_bot_token" >&2
  exit 1
fi

apt-get update
apt-get install -y --no-install-recommends python3 python3-venv python3-pip git curl ca-certificates libgomp1

if ! id devai >/dev/null 2>&1; then
  useradd --system --create-home --home-dir /opt/devai --shell /usr/sbin/nologin devai
fi

mkdir -p /opt/devai
if [ ! -d "$APP_DIR/.git" ]; then
  git clone "$REPO" "$APP_DIR"
else
  git -C "$APP_DIR" fetch origin
  git -C "$APP_DIR" pull --ff-only origin master
fi

python3 -m venv "$APP_DIR/.venv"
"$APP_DIR/.venv/bin/pip" install --upgrade pip
"$APP_DIR/.venv/bin/pip" install --no-cache-dir \
  --extra-index-url https://abetlen.github.io/llama-cpp-python/whl/cpu \
  -r "$APP_DIR/server/requirements.txt"

mkdir -p "$DATA_DIR" "$MODEL_DIR"
if [ ! -f "$MODEL" ]; then
  curl -L --retry 5 --retry-delay 5 -o "$MODEL" \
    "https://huggingface.co/bartowski/google_gemma-3-4b-it-GGUF/resolve/main/google_gemma-3-4b-it-Q4_K_M.gguf"
fi

printf '%s' "$TOKEN" > "$DATA_DIR/devai_ai_bot_token"
chmod 600 "$DATA_DIR/devai_ai_bot_token"

cat > "$DATA_DIR/devai_ai_bot.env" <<EOF
DEVAI_API_BASE=http://127.0.0.1:12001
TELEGRAM_CHANNEL=@devaiAI
TELEGRAM_ADMIN_IDS=${TELEGRAM_ADMIN_IDS:-}
TELEGRAM_COMMENTS_CHAT=${TELEGRAM_COMMENTS_CHAT:-}
DEVAI_CHANNEL_FREEFORM=1
DEVAI_CHANNEL_INTERVAL=600
DEVAI_CHANNEL_AI=0
DEVAI_AUTOPOST=1
DEVAI_PERSONA_ENABLED=1
DEVAI_PERSONA_INTERVAL=3600
DEVAI_COMMENTS_ENABLED=1
EOF
chmod 600 "$DATA_DIR/devai_ai_bot.env"

chown -R devai:devai /opt/devai

install -m 644 "$APP_DIR/deploy/devai-server.service" /etc/systemd/system/devai-server.service
install -m 644 "$APP_DIR/deploy/devai-bot.service" /etc/systemd/system/devai-bot.service
systemctl daemon-reload
systemctl enable --now devai-server.service
systemctl enable --now devai-bot.service

echo "DevAI server and bot installed."
echo "Check: systemctl status devai-server devai-bot"
echo "Logs:  journalctl -u devai-server -u devai-bot -f"
