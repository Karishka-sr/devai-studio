# DevAI Studio

**Android-студия для разработки с ИИ.** Чат с ведущими моделями, генерация Android-проектов, рабочее пространство с файлами и офлайн-модели, которые работают без интернета.

[Скачать APK](https://github.com/Karishka-sr/devai-studio/releases) · **v2.7.1** · Android 12+ · arm64-v8a

<p align="center">
  <a href="https://t.me/NeonBeatt">
    <img src="https://img.shields.io/badge/Telegram-@NeonBeatt-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram @NeonBeatt">
  </a>
  <a href="https://github.com/Karishka-sr/devai-studio/releases">
    <img src="https://img.shields.io/badge/Download_APK-v2.7.1-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Download APK">
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

### 🏗 Конструктор Vexel
- Полноценный визуальный редактор интерфейсов в стиле Material 3 Expressive.
- Проекты, слои, фигуры MD3, компоненты, маски, градиенты, тени и режим «Играть».
- Экспорт PNG/SVG, импорт SVG и генерация Compose-кода.
- Полный набор иконок Vexel: 23 670 SVG Material Symbols (outlined/rounded/sharp) в `assets/icons`.
- Баннер убран по запросу; в «Автор» осталась текстовая атрибуция, Telegram и сайт.
- Открывается из вкладки «Конструктор» на весь экран.
- Конструктор — Vexel, автор [@cheburek_dev](https://t.me/cheburek_dev), используется с разрешения.

### 🧩 Compose UI
- Чат, ИИ-провайдеры и Настройки полностью переписаны на Compose; старые XML-фрагменты и адаптеры удалены.
- Загрузочный экран при старте: логотип, «DevAI Studio», индикатор и плавный переход в приложение.
- Главный экран — Compose `Scaffold` + `NavigationBar`.
- Настоящие Material Icons (`material-icons-core`): навигация, чат, провайдеры, настройки — без emoji.
- Чат: `TopAppBar`, аватары, пустое состояние с подсказками, bottom sheet сессий, индикатор печати и иконочные кнопки.
- Material3 1.5.0-alpha10: `MaterialShapes`, Expressive-фигуры.

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
1. Скачайте `devai-studio-2.7.1.apk` в разделе [Releases](https://github.com/Karishka-sr/devai-studio/releases).
2. Разрешите установку из неизвестных источников.
3. Требования: Android 12+ (minSdk 31), процессор arm64-v8a.

Текущая сборка: **2.7.1** с конструктором Vexel (`devai-studio-2.7.1.apk`).

> 2.4.0 не использовать: в ней был вылет при открытии Настроек. Исправлено в 2.4.1+, актуальная — 2.7.1.

APK подписан самоподписанным ключом, поэтому Play Protect может показать предупреждение.

## Сборка
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/path/to/android-sdk

./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Требования: JDK 21, Android SDK 35, Gradle 8.9, AGP 8.6, Kotlin 2.2.20.
Compose: BOM 2026.03.01 (UI 1.10.6), Material3 1.5.0-alpha10 (`MaterialShapes`, `toShape`), `material-icons-core`, activity-compose 1.10.1, `androidx.graphics:graphics-shapes:1.1.0`.

Подпись release-сборки:
```bash
apksigner sign --ks release.jks --ks-key-alias devai \
  --out devai-studio.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

## Структура проекта
```
app/src/main/java/com/devai/studio/
├── ai/          # клиенты провайдеров, LiteRT-LM, форматы промптов
├── data/        # настройки, ключи, сессии, каталог моделей, Premium
├── ui/          # Compose-экраны: splash, чат, ИИ, настройки, автор
└── util/        # темы, диалоги, вспомогательные классы

app/src/main/kotlin/com/qwenz/qwel/   # Vexel: проекты, редактор, рендер, фигуры
```

## Технологии
- Kotlin 2.2, AndroidX, Material 3, Jetpack Compose UI (чат, ИИ, настройки)
- Compose BOM 2026.03.01 + Material3 1.5.0-alpha10, Vexel/Qwel constructor
- Kotlin Coroutines, OkHttp
- LiteRT-LM (инференс на устройстве)
- minSdk 31, targetSdk 34, arm64-v8a

## Ограничения
- Модели не входят в APK — скачиваются отдельно.
- Локальные модели в каталоге текстовые: vision/audio-бэкенды LiteRT-LM отключены.
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

## Благодарности
- Конструктор Vexel — [@cheburek_dev](https://t.me/cheburek_dev). Используется с разрешения автора. Оригинал: https://t.me/cheburek_dev

## Лицензии
- Модели: Apache-2.0 (HuggingFace `litert-community`).
- LiteRT-LM: лицензия Google AI Edge.
- Конструктор Vexel (Qwel): © @cheburek_dev, используется с разрешения.
- Шрифт Figtree: SIL OFL (`assets/fonts/Figtree-OFL.txt`).
- Шрифт Inter SA: SIL OFL; файл лицензии нужно положить в `assets/fonts` перед публикацией.
- Код приложения: © DevAI Studio.
