# DevAI Studio

**Android-студия для разработки с ИИ.** Чат с ведущими моделями, генерация Android-проектов, рабочее пространство с файлами и офлайн-модели, которые работают без интернета.

[Скачать APK](https://github.com/Karishka-sr/devai-studio/releases) · **v2.3** · Android 12+ · arm64-v8a

<p align="center">
  <a href="https://t.me/NeonBeatt">
    <img src="https://img.shields.io/badge/Telegram-@NeonBeatt-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram @NeonBeatt">
  </a>
  <a href="https://github.com/Karishka-sr/devai-studio/releases">
    <img src="https://img.shields.io/badge/Download_APK-v2.3-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Download APK">
  </a>
  <a href="https://rytmix-live.pplx-2.app">
    <img src="https://img.shields.io/badge/Site-rytmix--live-0A66C2?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Сайт">
  </a>
</p>

## Возможности

### 💬 Чат с ИИ
- 25+ провайдеров: OpenAI, Anthropic Claude, Google Gemini, DeepSeek, Groq, Mistral, OpenRouter, Together AI, Cerebras, NVIDIA NIM, xAI, Cohere, Perplexity и другие.
- Локальные серверы: Ollama, LM Studio, llama.cpp, Jan.
- Свой OpenAI-совместимый сервер.
- Стриминг ответов, история сессий, регенерация, Markdown и блоки кода с копированием.

### 📴 Офлайн-модели
Модели скачиваются внутрь приложения и работают **без интернета**. Инференс выполняется на устройстве через LiteRT-LM, данные не покидают телефон.

| Модель | Размер | Доступ |
|---|---|---|
| SmolLM 135M | ~159 МБ | Бесплатно |
| Qwen2.5 0.5B | ~521 МБ | Бесплатно |
| TinyLlama 1.1B | ~1.07 ГБ | Premium |
| Qwen2.5 1.5B | ~1.49 ГБ | Premium |

- Докачка после отмены, прогресс, удаление.
- Импорт своего `.task` и добавление модели по прямой ссылке.
- Выбор формата промпта: ChatML, Zephyr, Gemma, универсальный.

### 🏗 Генерация проектов
- Описание идеи → структура Android-проекта и готовые файлы.
- AI-генерация или шаблоны.
- Сохранение в рабочее пространство, просмотр и редактирование файлов.

### 🎨 Оформление
- **Material Design 3** — бесплатно, с dynamic color на Android 12+.
- **Material Expressive** — крупные скругления и яркая палитра.
- **Старый вид 2000–2020** — классический синий/серый интерфейс.
- Переключение: Настройки → Оформление.

### 👑 Premium
- Премиум-стили оформления.
- Большие офлайн-модели: TinyLlama 1.1B и Qwen2.5 1.5B.
- Экспорт чата в Markdown.
- Активация промокодом. Оплата через Google Play Billing — в планах.

## Установка
1. Скачайте `devai-studio-2.3.apk` в разделе [Releases](https://github.com/Karishka-sr/devai-studio/releases).
2. Разрешите установку из неизвестных источников.
3. Требования: Android 12+ (minSdk 31), процессор arm64-v8a.

APK подписан самоподписанным ключом, поэтому Play Protect может показать предупреждение.

## Сборка
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/path/to/android-sdk

./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Подпись release-сборки:
```bash
apksigner sign --ks release.jks --ks-key-alias devai \
  --out devai-studio.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

Требования: JDK 21, Android SDK 34, Kotlin 2.1.

## Структура проекта
```
app/src/main/java/com/devai/studio/
├── ai/          # клиенты провайдеров, LiteRT-LM, форматы промптов
├── builder/     # генерация Android-проектов
├── data/        # настройки, ключи, сессии, каталог моделей, Premium
├── ui/          # экраны и адаптеры
└── util/        # темы, диалоги, вспомогательные классы
```

## Технологии
- Kotlin 2.1, AndroidX, Material 3, ViewBinding
- Kotlin Coroutines, OkHttp
- LiteRT-LM (инференс на устройстве)
- minSdk 31, targetSdk 34, arm64-v8a

## Ограничения
- Модели не входят в APK — скачиваются отдельно.
- Небольшие офлайн-модели подходят для простых задач; для сложного кода лучше облачные модели.
- Для Qwen2.5 1.5B рекомендуется 6 ГБ RAM.
- Premium и промокоды проверяются локально. Для продакшена нужен сервер или Google Play Billing.


## 💬 Сообщество

<p>
  <a href="https://t.me/NeonBeatt">
    <img src="https://img.shields.io/badge/Telegram-@NeonBeatt-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram @NeonBeatt">
  </a>
  <a href="https://rytmix-live.pplx-2.app">
    <img src="https://img.shields.io/badge/Site-rytmix--live-0A66C2?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Сайт">
  </a>
</p>

Новости, обновления и поддержка — в Telegram-канале [@NeonBeatt](https://t.me/NeonBeatt). Сайт: [rytmix-live.pplx-2.app](https://rytmix-live.pplx-2.app).

## Лицензии
- Модели: Apache-2.0 (HuggingFace `litert-community`).
- LiteRT-LM: лицензия Google AI Edge.
- Код приложения: © DevAI Studio.
