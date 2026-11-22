package com.devai.studio.data

enum class PremiumPlan(
    val id: String,
    val rank: Int,
    val days: Int,
    val stars: Int,
    val title: String,
    val duration: String,
    val tagline: String,
) {
    FREE("free", 0, 0, 0, "Бесплатно", "без ограничения", "Базовые возможности"),
    WEEK("week", 1, 7, 75, "Неделя", "7 дней", "Попробовать Premium"),
    MONTH("month", 2, 30, 249, "Месяц", "30 дней", "Для активной разработки"),
    YEAR("year", 3, 365, 1490, "Год", "365 дней", "Лучшая цена за месяц"),
    LIFETIME("lifetime", 4, 0, 4990, "Навсегда", "навсегда", "Все функции и будущие обновления");

    val isPaid: Boolean get() = this != FREE
    val isTemporary: Boolean get() = this != FREE && this != LIFETIME
}

enum class PremiumFeature(
    val title: String,
    val description: String,
    val minPlan: PremiumPlan,
    val available: Boolean = true,
) {
    EXPRESSIVE_THEME("Expressive-тема", "Современная тема Material Expressive", PremiumPlan.WEEK),
    HAPTIC("Вибрация при отправке", "Тактильный отклик на действия", PremiumPlan.WEEK),
    CHAT_EXPORT("Экспорт чата", "Сохранить диалог в Markdown", PremiumPlan.WEEK),
    LFM_1_2("LFM2.5 1.2B", "Быстрая локальная модель для чата", PremiumPlan.WEEK),
    TINYLLAMA("TinyLlama 1.1B", "Компактная локальная модель", PremiumPlan.WEEK),
    QWEN_1_5("Qwen2.5 1.5B", "Локальная модель с хорошим русским", PremiumPlan.WEEK),
    DOWNLOAD_RESUME("Докачка моделей", "Продолжение загрузки после отмены", PremiumPlan.WEEK),

    RETRO_THEME("Retro-тема", "Тема «Старый вид»", PremiumPlan.MONTH),
    ACCENT_COLORS("10 акцентных цветов", "Настройка цвета интерфейса", PremiumPlan.MONTH),
    CHAT_FONTS("Шрифты чата", "Системный, с засечками и моноширинный", PremiumPlan.MONTH),
    BUBBLE_WIDTH("Ширина сообщений", "Настройка ширины пузырей", PremiumPlan.MONTH),
    AVATARS("Аватары", "Иконки пользователя и DevAI", PremiumPlan.MONTH),
    COMPACT_CHAT("Компактный чат", "Меньше отступов между сообщениями", PremiumPlan.MONTH),
    CODE_WRAP("Перенос кода", "Переносить длинные строки кода", PremiumPlan.MONTH),
    PROMPT_PRESETS("Пресеты промптов", "Быстрые готовые системные промпты", PremiumPlan.MONTH),
    PROMPT_LIBRARY("Библиотека промптов", "24 готовых промпта для разработки", PremiumPlan.MONTH),
    SEARCH("Веб-поиск", "Поиск актуальной информации в чате", PremiumPlan.MONTH),
    CUSTOM_PROVIDER("Свой провайдер", "Свой Base URL и модель", PremiumPlan.MONTH),
    CHARACTERS("Персонажи", "Карточки с характером и промптом", PremiumPlan.MONTH),
    QWEN_CODER("Qwen2.5 Coder 1.5B", "Локальная модель для кода", PremiumPlan.MONTH),
    QWEN3_1_7("Qwen3 1.7B", "Сильная локальная модель", PremiumPlan.MONTH),

    CODE_FONT("Размер кода", "Настройка размера кода в чате", PremiumPlan.YEAR),
    TTS("Озвучивание ответов", "Text-to-speech для сообщений", PremiumPlan.YEAR),
    HISTORY_UNLIMITED("Вся история", "Лимит истории 0 — без ограничений", PremiumPlan.YEAR),
    LOCAL_CONTEXT("Контекст локальных моделей", "Настройка контекста и Top-K", PremiumPlan.YEAR),
    SYSTEM_PROMPT("Свой системный промпт", "Полный контроль над поведением", PremiumPlan.YEAR),
    SETTINGS_BACKUP("Бэкап настроек", "Экспорт всех настроек в JSON", PremiumPlan.YEAR),
    CHAT_BACKUP("Бэкап всех чатов", "Экспорт всех сессий в JSON", PremiumPlan.YEAR),
    MULTI_EXPORT("Экспорт всех сессий", "Один файл со всеми диалогами", PremiumPlan.YEAR),
    QR_EXPORT("QR-перенос настроек", "Перенос настроек по QR-коду", PremiumPlan.YEAR),
    DEEPSEEK("DeepSeek R1 1.5B", "Локальные рассуждения для кода", PremiumPlan.YEAR),
    SMOLLM3("SmolLM3 3B", "Крупная локальная модель", PremiumPlan.YEAR),
    PHI4("Phi-4 Mini", "Сильная локальная модель Microsoft", PremiumPlan.YEAR),
    EARLY_ACCESS("Ранний доступ", "Новые функции раньше остальных", PremiumPlan.YEAR),
    PRIORITY_MODELS("Приоритет моделей", "Новые модели в каталоге первыми", PremiumPlan.YEAR),

    MCP("MCP-серверы", "Подключение внешних инструментов", PremiumPlan.LIFETIME),
    PLUGINS("Плагины", "Установка и запуск плагинов", PremiumPlan.LIFETIME),
    PLUGIN_MARKETPLACE("Маркетплейс плагинов", "Каталог плагинов в приложении", PremiumPlan.LIFETIME, false),
    CUSTOM_THEME("Редактор тем", "Свои цвета и стили", PremiumPlan.LIFETIME, false),
    AGENT_MODE("Агентный режим", "Планирование и выполнение задач", PremiumPlan.LIFETIME, false),
    MULTI_AGENT("Мульти-агенты", "Несколько агентов в одном чате", PremiumPlan.LIFETIME, false),
    GITHUB("GitHub-интеграция", "Issues, PR и код из чата", PremiumPlan.LIFETIME, false),
    GITLAB("GitLab-интеграция", "Issues и merge requests", PremiumPlan.LIFETIME, false),
    APK_BUILD("Сборка APK", "Сборка проекта прямо из чата", PremiumPlan.LIFETIME, false),
    PROJECT_EXPORT("Экспорт проекта", "Готовый проект одним архивом", PremiumPlan.LIFETIME, false),
    CLOUD_SYNC("Облачная синхронизация", "Чаты и настройки на всех устройствах", PremiumPlan.LIFETIME, false),
    VOICE_CHAT("Голосовой чат", "Разговор с моделью голосом", PremiumPlan.LIFETIME, false),
    VISION("Vision-модели", "Анализ изображений", PremiumPlan.LIFETIME, false),
    IMAGE_GEN("Генерация изображений", "Картинки по описанию", PremiumPlan.LIFETIME, false),
    DOCUMENTS("Анализ документов", "PDF, DOCX и таблицы", PremiumPlan.LIFETIME, false),
    TEAM("Командные места", "Доступ для команды", PremiumPlan.LIFETIME, false),
    API_ACCESS("API-доступ", "Управление DevAI из других приложений", PremiumPlan.LIFETIME, false),
    WHITE_LABEL("White-label", "Свой бренд и иконка", PremiumPlan.LIFETIME, false),
    COMMERCIAL("Коммерческое использование", "Для рабочих проектов и клиентов", PremiumPlan.LIFETIME, false),
    PERSONAL_SUPPORT("Личная поддержка", "Помощь автора в Telegram", PremiumPlan.LIFETIME, false),
    FUTURE_MODELS("Все будущие модели", "Новые локальные модели навсегда", PremiumPlan.LIFETIME, false),
    FUTURE_THEMES("Все будущие темы", "Новые темы и оформления навсегда", PremiumPlan.LIFETIME, false),
    LIFETIME_UPDATES("Обновления навсегда", "Все будущие версии Premium", PremiumPlan.LIFETIME, false),
    BETA("Бета-функции", "Экспериментальные возможности", PremiumPlan.LIFETIME, false),
    ;

    companion object {
        fun forPlan(plan: PremiumPlan): List<PremiumFeature> =
            entries.filter { it.minPlan.rank <= plan.rank }

        fun newInPlan(plan: PremiumPlan): List<PremiumFeature> =
            entries.filter { it.minPlan == plan }
    }
}
