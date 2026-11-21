package com.devai.studio.data

import com.devai.studio.ai.AiFormat

/**
 * Каталог ИИ-провайдеров с категориями и ссылками.
 * Категории: ПОЛНОСТЬЮ БЕСПЛАТНЫЕ / БЕСПЛАТНЫЕ (тариф) / ПЛАТНЫЕ.
 * Списки моделей могут меняться — актуальные смотрите по ссылке «Сайт».
 */
object ProviderCatalog {

    val all: List<ProviderInfo> = listOf(

        // ===================== ПОЛНОСТЬЮ БЕСПЛАТНЫЕ (локальные) =====================

        ProviderInfo(
            id = "local",
            name = "Локальная модель (офлайн)",
            category = Category.FULLY_FREE,
            description = "Модель скачивается в приложение и работает без интернета. Данные не покидают телефон.",
            format = AiFormat.LOCAL,
            baseUrl = "",
            models = listOf("модель из раздела «Локальные модели»"),
            getKeyUrl = "",
            website = "https://huggingface.co/litert-community",
            freeNote = "Откройте «Локальные модели» и скачайте модель. Работает офлайн."
        ),

        ProviderInfo(
            id = "ollama",
            name = "Ollama (локально)",
            category = Category.FULLY_FREE,
            description = "Полностью бесплатно, работает на вашем компьютере, без интернета.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "http://10.0.2.2:11434/v1",
            models = listOf(
                "llama3.3", "llama3.2", "llama3.1", "llama3", "llama2",
                "qwen2.5", "qwen2.5-coder", "qwen2", "deepseek-r1", "deepseek-coder-v2",
                "mistral", "mistral-nemo", "mixtral", "gemma3", "gemma2",
                "phi4", "phi3", "codellama", "starcoder2", "yi", "glm4",
                "granite3.1-moe", "llava", "bakllava"
            ),
            getKeyUrl = "https://ollama.com/download",
            website = "https://ollama.com",
            freeNote = "В эмуляторе Android адрес 10.0.2.2, на телефоне — IP компьютера в Wi-Fi."
        ),

        ProviderInfo(
            id = "lmstudio",
            name = "LM Studio (локально)",
            category = Category.FULLY_FREE,
            description = "Локальный сервер моделей с OpenAI-совместимым API. Бесплатно.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "http://10.0.2.2:1234/v1",
            models = listOf(
                "llama-3.3-70b-instruct", "qwen2.5-coder-32b", "mistral-small",
                "deepseek-r1-distill", "gemma-3-27b", "phi-4", "любая загруженная модель"
            ),
            getKeyUrl = "https://lmstudio.ai",
            website = "https://lmstudio.ai",
            freeNote = "Включите локальный сервер (Developer → Start Server)."
        ),

        ProviderInfo(
            id = "llama-cpp",
            name = "llama.cpp (локально)",
            category = Category.FULLY_FREE,
            description = "Лёгкий локальный сервер для .gguf-моделей. Полностью бесплатно.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "http://10.0.2.2:8080/v1",
            models = listOf("локальная gguf-модель"),
            getKeyUrl = "https://github.com/ggml-org/llama.cpp",
            website = "https://github.com/ggml-org/llama.cpp",
            freeNote = "Запустите: llama-server -m model.gguf --port 8080"
        ),

        ProviderInfo(
            id = "jan",
            name = "Jan (локально)",
            category = Category.FULLY_FREE,
            description = "Открытое локальное приложение с OpenAI-совместимым API.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "http://10.0.2.2:1337/v1",
            models = listOf("любая локальная модель"),
            getKeyUrl = "https://jan.ai",
            website = "https://jan.ai",
            freeNote = "Включите локальный API-сервер в настройках Jan."
        ),

        ProviderInfo(
            id = "openrouter-free",
            name = "OpenRouter — бесплатные модели",
            category = Category.FULLY_FREE,
            description = "Бесплатные модели OpenRouter (с суффиксом :free). Один ключ на всё.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://openrouter.ai/api/v1",
            models = listOf(
                "deepseek/deepseek-r1:free", "deepseek/deepseek-chat-v3-0324:free",
                "google/gemini-2.0-flash-exp:free", "google/gemma-3-27b-it:free",
                "meta-llama/llama-3.3-70b-instruct:free", "meta-llama/llama-3.2-3b-instruct:free",
                "qwen/qwen-2.5-coder-32b-instruct:free", "qwen/qwq-32b:free",
                "mistralai/mistral-7b-instruct:free", "microsoft/phi-4:free",
                "nvidia/llama-3.1-nemotron-70b-instruct:free", "moonshotai/kimi-k2:free"
            ),
            getKeyUrl = "https://openrouter.ai/keys",
            website = "https://openrouter.ai/models?max_price=0",
            freeNote = "Лимиты на бесплатные модели, но оплата не нужна."
        ),

        // ===================== БЕСПЛАТНЫЕ (есть бесплатный тариф) =====================

        ProviderInfo(
            id = "google-gemini",
            name = "Google Gemini",
            category = Category.FREE,
            description = "Бесплатный тариф в Google AI Studio с дневными лимитами.",
            format = AiFormat.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            models = listOf(
                "gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite",
                "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-2.0-flash-exp",
                "gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.5-flash-8b",
                "gemma-3-27b-it", "gemma-3-12b-it"
            ),
            getKeyUrl = "https://aistudio.google.com/app/apikey",
            website = "https://ai.google.dev/gemini-api/docs/pricing",
            freeNote = "Бесплатный уровень имеет ограничения по запросам в минуту/день."
        ),

        ProviderInfo(
            id = "groq",
            name = "Groq",
            category = Category.FREE,
            description = "Очень быстрый инференс. Есть бесплатный тариф с лимитами.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.groq.com/openai/v1",
            models = listOf(
                "llama-3.3-70b-versatile", "llama-3.1-8b-instant", "llama3-70b-8192",
                "llama3-8b-8192", "llama-3.2-1b-preview", "llama-3.2-3b-preview",
                "llama-3.2-11b-vision-preview", "llama-3.2-90b-vision-preview",
                "mixtral-8x7b-32768", "gemma2-9b-it", "deepseek-r1-distill-llama-70b",
                "qwen-2.5-32b", "qwen-2.5-coder-32b", "whisper-large-v3", "whisper-large-v3-turbo"
            ),
            getKeyUrl = "https://console.groq.com/keys",
            website = "https://console.groq.com/docs/models",
            freeNote = "Бесплатные лимиты по токенам в минуту/день."
        ),

        ProviderInfo(
            id = "mistral",
            name = "Mistral AI",
            category = Category.FREE,
            description = "Европейский провайдер. Бесплатный experimental-тариф с лимитами.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.mistral.ai/v1",
            models = listOf(
                "mistral-large-latest", "mistral-medium-latest", "mistral-small-latest",
                "open-mistral-7b", "open-mixtral-8x7b", "open-mixtral-8x22b",
                "codestral-latest", "ministral-3b-latest", "ministral-8b-latest",
                "pixtral-large-latest", "pixtral-12b-2409", "mistral-nemo",
                "mistral-saba-latest", "magistral-medium-latest"
            ),
            getKeyUrl = "https://console.mistral.ai/api-keys/",
            website = "https://docs.mistral.ai/getting-started/models/",
            freeNote = "Уровень «Experiment» бесплатен, но с ограничениями по запросам."
        ),

        ProviderInfo(
            id = "together",
            name = "Together AI",
            category = Category.FREE,
            description = "Много открытых моделей. Есть бесплатный тариф с лимитами.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.together.xyz/v1",
            models = listOf(
                "meta-llama/Llama-3.3-70B-Instruct-Turbo",
                "meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo",
                "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo",
                "deepseek-ai/DeepSeek-R1", "deepseek-ai/DeepSeek-V3",
                "Qwen/Qwen2.5-72B-Instruct-Turbo", "Qwen/Qwen2.5-Coder-32B-Instruct",
                "mistralai/Mixtral-8x7B-Instruct-v0.1", "google/gemma-2-27b-it",
                "nvidia/Llama-3.1-Nemotron-70B-Instruct-HF"
            ),
            getKeyUrl = "https://api.together.ai/settings/api-keys",
            website = "https://www.together.ai/pricing",
            freeNote = "Стартовые бесплатные кредиты и лимиты на часть моделей."
        ),

        ProviderInfo(
            id = "cerebras",
            name = "Cerebras",
            category = Category.FREE,
            description = "Сверхбыстрый инференс. Бесплатный тариф с лимитами.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.cerebras.ai/v1",
            models = listOf(
                "llama3.3-70b", "llama3.1-8b", "llama-3.3-70b",
                "qwen-3-32b", "deepseek-r1-distill-llama-70b"
            ),
            getKeyUrl = "https://cloud.cerebras.ai/",
            website = "https://inference-docs.cerebras.ai/models/overview",
            freeNote = "Бесплатный уровень с ограничением запросов в день."
        ),

        ProviderInfo(
            id = "github-models",
            name = "GitHub Models",
            category = Category.FREE,
            description = "Бесплатный доступ к моделям для разработчиков через GitHub-токен.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://models.inference.ai.azure.com",
            models = listOf(
                "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini", "o1", "o1-mini",
                "o3-mini", "DeepSeek-R1", "DeepSeek-V3-0324", "Llama-3.3-70B-Instruct",
                "Phi-4", "Mistral-large-2411", "Cohere-command-r-plus-08-2024"
            ),
            getKeyUrl = "https://github.com/settings/tokens",
            website = "https://github.com/marketplace/models",
            freeNote = "Ключ — ваш GitHub Personal Access Token (classic, scope read:models)."
        ),

        ProviderInfo(
            id = "nvidia",
            name = "NVIDIA NIM",
            category = Category.FREE,
            description = "Каталог моделей NVIDIA. Бесплатные кредиты для разработчиков.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://integrate.api.nvidia.com/v1",
            models = listOf(
                "meta/llama-3.3-70b-instruct", "deepseek-ai/deepseek-r1",
                "qwen/qwen2.5-coder-32b-instruct", "nvidia/llama-3.1-nemotron-70b-instruct",
                "mistralai/mixtral-8x22b-instruct-v0.1", "google/gemma-2-27b-it"
            ),
            getKeyUrl = "https://build.nvidia.com",
            website = "https://build.nvidia.com/models",
            freeNote = "Бесплатные кредиты API для разработчиков."
        ),

        ProviderInfo(
            id = "sambanova",
            name = "SambaNova",
            category = Category.FREE,
            description = "Быстрый инференс открытых моделей. Бесплатный тариф.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.sambanova.ai/v1",
            models = listOf(
                "Meta-Llama-3.3-70B-Instruct", "DeepSeek-R1", "Qwen2.5-72B-Instruct",
                "Llama-4-Maverick-17B-128E-Instruct"
            ),
            getKeyUrl = "https://cloud.sambanova.ai/apis",
            website = "https://cloud.sambanova.ai/",
            freeNote = "Бесплатный уровень с ограничениями."
        ),

        ProviderInfo(
            id = "huggingface",
            name = "Hugging Face",
            category = Category.FREE,
            description = "Inference Providers — сотни моделей через один OpenAI-совместимый API.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://router.huggingface.co/v1",
            models = listOf(
                "meta-llama/Llama-3.3-70B-Instruct", "Qwen/Qwen2.5-72B-Instruct",
                "deepseek-ai/DeepSeek-R1", "mistralai/Mistral-7B-Instruct-v0.3"
            ),
            getKeyUrl = "https://huggingface.co/settings/tokens",
            website = "https://huggingface.co/docs/inference-providers",
            freeNote = "Небольшой бесплатный лимит в месяц, далее — платно."
        ),

        ProviderInfo(
            id = "custom",
            name = "Свой сервер (любой ИИ)",
            category = Category.FREE,
            description = "Любой OpenAI-совместимый endpoint: шлюзы, Azure, корпоративные серверы.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "",
            models = listOf("своя модель"),
            getKeyUrl = "",
            website = "",
            freeNote = "Задайте base URL и модель в Настройках (вкладка «Настройки»)."
        ),

        // ===================== ПЛАТНЫЕ =====================

        ProviderInfo(
            id = "openai",
            name = "OpenAI",
            category = Category.PAID,
            description = "GPT-модели, o-серия. Оплата по токенам.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.openai.com/v1",
            models = listOf(
                "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini", "gpt-4.1-nano",
                "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo", "o1", "o1-mini", "o1-pro",
                "o3", "o3-mini", "o4-mini", "gpt-5", "gpt-5-mini", "gpt-5-nano"
            ),
            getKeyUrl = "https://platform.openai.com/api-keys",
            website = "https://openai.com/api/pricing/"
        ),

        ProviderInfo(
            id = "anthropic",
            name = "Anthropic Claude",
            category = Category.PAID,
            description = "Модели Claude. Оплата по токенам.",
            format = AiFormat.ANTHROPIC,
            baseUrl = "https://api.anthropic.com",
            models = listOf(
                "claude-opus-4-20250514", "claude-sonnet-4-20250514",
                "claude-sonnet-4-5-20250929", "claude-opus-4-1-20250805",
                "claude-haiku-4-5-20251001", "claude-3-7-sonnet-20250219",
                "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022",
                "claude-3-opus-20240229", "claude-3-haiku-20240307"
            ),
            getKeyUrl = "https://console.anthropic.com/settings/keys",
            website = "https://www.anthropic.com/pricing"
        ),

        ProviderInfo(
            id = "deepseek",
            name = "DeepSeek",
            category = Category.PAID,
            description = "Очень дешёвые модели для кода и рассуждений.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.deepseek.com/v1",
            models = listOf("deepseek-chat", "deepseek-reasoner", "deepseek-coder"),
            getKeyUrl = "https://platform.deepseek.com/api_keys",
            website = "https://api-docs.deepseek.com/quick_start/pricing"
        ),

        ProviderInfo(
            id = "xai",
            name = "xAI (Grok)",
            category = Category.PAID,
            description = "Модели Grok от xAI.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.x.ai/v1",
            models = listOf(
                "grok-4", "grok-4-fast", "grok-3", "grok-3-mini",
                "grok-2-1212", "grok-2-vision-1212", "grok-beta"
            ),
            getKeyUrl = "https://console.x.ai/",
            website = "https://docs.x.ai/docs/models"
        ),

        ProviderInfo(
            id = "cohere",
            name = "Cohere",
            category = Category.PAID,
            description = "Модели Command. Есть OpenAI-совместимый endpoint.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.cohere.ai/compatibility/v1",
            models = listOf(
                "command-a-03-2025", "command-r-plus", "command-r-plus-08-2024",
                "command-r", "command-r7b", "command-light"
            ),
            getKeyUrl = "https://dashboard.cohere.com/api-keys",
            website = "https://cohere.com/pricing"
        ),

        ProviderInfo(
            id = "ai21",
            name = "AI21 Labs",
            category = Category.PAID,
            description = "Модели Jamba с большим контекстом.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.ai21.com/studio/v1",
            models = listOf(
                "jamba-large-1.7", "jamba-mini-1.7", "jamba-1.5-large", "jamba-1.5-mini"
            ),
            getKeyUrl = "https://studio.ai21.com/apikey",
            website = "https://www.ai21.com/pricing"
        ),

        ProviderInfo(
            id = "moonshot",
            name = "Moonshot AI (Kimi)",
            category = Category.PAID,
            description = "Модели Kimi с длинным контекстом.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.moonshot.ai/v1",
            models = listOf(
                "kimi-k2-0711-preview", "moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k"
            ),
            getKeyUrl = "https://platform.moonshot.ai/console/api-keys",
            website = "https://platform.moonshot.ai/docs/pricing"
        ),

        ProviderInfo(
            id = "zhipu",
            name = "Zhipu AI (GLM)",
            category = Category.PAID,
            description = "Модели GLM (ChatGLM).",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            models = listOf(
                "glm-4.6", "glm-4.5", "glm-4-plus", "glm-4", "glm-4-air", "glm-4-flash"
            ),
            getKeyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
            website = "https://open.bigmodel.cn/pricing"
        ),

        ProviderInfo(
            id = "dashscope",
            name = "Alibaba Qwen (DashScope)",
            category = Category.PAID,
            description = "Модели Qwen через OpenAI-совместимый режим.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
            models = listOf(
                "qwen-max", "qwen-plus", "qwen-turbo", "qwen3-235b-a22b",
                "qwen2.5-72b-instruct", "qwen2.5-coder-32b-instruct"
            ),
            getKeyUrl = "https://dashscope.console.aliyun.com/apiKey",
            website = "https://www.alibabacloud.com/help/en/model-studio/models"
        ),

        ProviderInfo(
            id = "perplexity",
            name = "Perplexity",
            category = Category.PAID,
            description = "Модели Sonar с доступом к веб-поиску.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.perplexity.ai",
            models = listOf(
                "sonar", "sonar-pro", "sonar-reasoning", "sonar-reasoning-pro",
                "sonar-deep-research"
            ),
            getKeyUrl = "https://www.perplexity.ai/settings/api",
            website = "https://docs.perplexity.ai/getting-started/models"
        ),

        ProviderInfo(
            id = "fireworks",
            name = "Fireworks AI",
            category = Category.PAID,
            description = "Быстрый хостинг открытых моделей.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.fireworks.ai/inference/v1",
            models = listOf(
                "accounts/fireworks/models/llama-v3p3-70b-instruct",
                "accounts/fireworks/models/deepseek-r1",
                "accounts/fireworks/models/qwen2p5-coder-32b-instruct",
                "accounts/fireworks/models/mixtral-8x22b-instruct"
            ),
            getKeyUrl = "https://fireworks.ai/account/api-keys",
            website = "https://fireworks.ai/pricing"
        ),

        ProviderInfo(
            id = "deepinfra",
            name = "DeepInfra",
            category = Category.PAID,
            description = "Дешёвый хостинг открытых моделей.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://api.deepinfra.com/v1/openai",
            models = listOf(
                "meta-llama/Meta-Llama-3.1-405B-Instruct",
                "meta-llama/Llama-3.3-70B-Instruct", "deepseek-ai/DeepSeek-R1",
                "Qwen/Qwen2.5-Coder-32B-Instruct", "mistralai/Mixtral-8x7B-Instruct-v0.1"
            ),
            getKeyUrl = "https://deepinfra.com/dash/api_keys",
            website = "https://deepinfra.com/pricing"
        ),

        ProviderInfo(
            id = "openrouter",
            name = "OpenRouter (все модели)",
            category = Category.PAID,
            description = "Единый ключ к сотням моделей всех провайдеров.",
            format = AiFormat.OPENAI_COMPAT,
            baseUrl = "https://openrouter.ai/api/v1",
            models = listOf(
                "openai/gpt-4o", "openai/gpt-4.1", "anthropic/claude-sonnet-4",
                "anthropic/claude-opus-4", "google/gemini-2.5-pro",
                "deepseek/deepseek-r1", "meta-llama/llama-3.3-70b-instruct",
                "qwen/qwen-2.5-coder-32b-instruct", "x-ai/grok-4"
            ),
            getKeyUrl = "https://openrouter.ai/keys",
            website = "https://openrouter.ai/models"
        )
    )

    private val byId: Map<String, ProviderInfo> = all.associateBy { it.id }

    fun find(id: String?): ProviderInfo? = id?.let { byId[it] }

    fun byCategory(category: Category): List<ProviderInfo> =
        all.filter { it.category == category }
}
