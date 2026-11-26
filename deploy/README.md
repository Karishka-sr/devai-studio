# DevAI на постоянном сервере

Процессы внутри этой рабочей сессии живут только пока сессия открыта. Для
постов «как ChatGPT/Claude» без сессии нужен внешний хост.

## Вариант 1. VPS (рекомендуется, 24/7)

Минимум: 2 vCPU, 4 ГБ RAM, 10 ГБ диска. Лучше 8 ГБ RAM.

На Ubuntu/Debian:

```bash
ssh root@ВАШ_СЕРВЕР

# токен @devaiAI_bot от BotFather
sudo TELEGRAM_BOT_TOKEN='123456:ABC...' \
  TELEGRAM_ADMIN_IDS='9149969964' \
  TELEGRAM_COMMENTS_CHAT='-1005674856729' \
  bash deploy/install-vps.sh
```

Если репозитория на сервере ещё нет:

```bash
git clone https://github.com/Karishka-sr/devai-studio.git /opt/devai/app
cd /opt/devai/app
sudo TELEGRAM_BOT_TOKEN='123456:ABC...' bash deploy/install-vps.sh
```

Скрипт создаёт пользователя `devai`, venv, скачивает Gemma 4B, ставит
`devai-server.service` и `devai-bot.service` с автозапуском и
`Restart=always`.

Проверка:

```bash
systemctl status devai-server devai-bot
journalctl -u devai-server -u devai-bot -f
curl -s http://127.0.0.1:12001/health
```

После этого бот постит и отвечает даже когда эта сессия закрыта. Сервер
перезагрузится — systemd поднимет оба процесса.

## Вариант 2. Docker Compose

Нужны Docker и Compose. Положите токен в
`server/data/devai_ai_bot_token`, создайте `server/data/devai_ai_bot.env`
(пример — в `deploy/devai_ai_bot.env.example`), положите модель в
`server/models/`.

```bash
docker compose -f deploy/docker-compose.yml up -d --build
docker compose -f deploy/docker-compose.yml logs -f
```

## Вариант 3. GitHub Actions (без VPS)

В `.github/workflows/devai-bot.yml` есть запуск каждые 6 часов на 5 часов.
Это не круглосуточный сервер, но посты идут без открытой сессии.

Нужно один раз добавить в GitHub:

`Settings → Secrets and variables → Actions → New repository secret`

- `TELEGRAM_BOT_TOKEN` — токен @devaiAI_bot;
- `TELEGRAM_ADMIN_IDS` — id админа;
- `TELEGRAM_COMMENTS_CHAT` — id группы комментариев (если нужна).

После этого запустить workflow вручную: `Actions → DevAI channel bot →
Run workflow`.

Ограничение: GitHub Actions даёт максимум 6 часов на job, поэтому бот
работает не абсолютно непрерывно. Для 24/7 нужен VPS.
