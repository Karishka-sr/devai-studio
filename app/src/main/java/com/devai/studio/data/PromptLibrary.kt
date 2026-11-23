package com.devai.studio.data

data class PromptTemplate(val title: String, val prompt: String, val category: String = "")

object PromptLibrary {
    private val builtIn: List<PromptTemplate> = listOf(
        PromptTemplate(
            "Senior Android",
            "Ты senior Android-разработчик. Отвечай на Kotlin, используй Jetpack Compose, " +
                "Coroutines и Material 3. Давай готовый код и объясняй риск изменений."
        ),
        PromptTemplate(
            "Kotlin-ревью",
            "Проведи ревью Kotlin-кода: найди баги, утечки, проблемы с coroutines, null-safety " +
                "и производительностью. Предложи исправленный код."
        ),
        PromptTemplate(
            "Compose UI",
            "Ты эксперт по Jetpack Compose. Собери аккуратный экран по описанию, " +
                "используй Material 3, состояния и preview. Не добавляй лишние зависимости."
        ),
        PromptTemplate(
            "Архитектор",
            "Ты архитектор Android-приложения. Предложи структуру модулей, слои данных, " +
                "DI и навигацию. Сравни 2 варианта и укажи trade-offs."
        ),
        PromptTemplate(
            "Баги и краши",
            "Помоги найти причину краша. Задай минимальные уточнения, предложи гипотезы " +
                "по вероятности и конкретные шаги проверки. Затем дай патч."
        ),
        PromptTemplate(
            "Unit-тесты",
            "Напиши unit-тесты на Kotlin/JUnit для кода ниже. Покрой успешные сценарии, " +
                "граничные случаи и ошибки. Не используй mocks без необходимости."
        ),
        PromptTemplate(
            "Gradle-доктор",
            "Ты эксперт по Gradle и AGP. Найди причину ошибки сборки, объясни её и дай " +
                "минимальный рабочий фикс для build.gradle.kts."
        ),
        PromptTemplate(
            "API-клиент",
            "Сгенерируй Kotlin API-клиент на OkHttp/Retrofit: DTO, интерфейс, обработку " +
                "ошибок, таймауты и пример использования."
        ),
        PromptTemplate(
            "Room-схема",
            "Спроектируй Room-базу: entity, DAO, индексы, миграции и репозиторий. " +
                "Объясни, почему выбрана такая схема."
        ),
        PromptTemplate(
            "Оптимизация",
            "Проанализируй производительность Android-экрана: recomposition, память, " +
                "сеть и батарею. Дай список изменений по приоритету."
        ),
        PromptTemplate(
            "Безопасность",
            "Проверь код на безопасность: хранение токенов, логирование, WebView, " +
                "intent-ы и сетевые запросы. Дай исправления."
        ),
        PromptTemplate(
            "Python-скрипт",
            "Напиши production-ready Python-скрипт с argparse, логированием, обработкой " +
                "ошибок и type hints. Добавь пример запуска."
        ),
        PromptTemplate(
            "SQL-оптимизация",
            "Оптимизируй SQL-запрос: покажи план, индексы, перепиши запрос и объясни " +
                "выигрыш. Учти PostgreSQL."
        ),
        PromptTemplate(
            "React-компонент",
            "Создай React-компонент на TypeScript с доступностью, состояниями загрузки " +
                "и ошибок. Стили — Tailwind. Без лишних библиотек."
        ),
        PromptTemplate(
            "README",
            "Напиши README для проекта: что делает, стек, установка, запуск, " +
                "переменные окружения, скриншоты и лицензия. Кратко и по делу."
        ),
        PromptTemplate(
            "Commit message",
            "Сгенерируй Conventional Commits сообщение для diff. Дай заголовок до 72 " +
                "символов и тело с причиной изменения."
        ),
        PromptTemplate(
            "PR-описание",
            "Составь описание pull request: проблема, решение, изменения, риски, " +
                "как тестировать и чек-лист ревью."
        ),
        PromptTemplate(
            "Документация API",
            "Опиши API в OpenAPI 3.1: эндпоинты, схемы, коды ошибок и примеры. " +
                "Не придумывай поля, которых нет в коде."
        ),
        PromptTemplate(
            "Рефакторинг",
            "Отрефактори код без изменения поведения: убери дубли, уменьши вложенность, " +
                "выдели функции. Покажи diff и объясни каждое изменение."
        ),
        PromptTemplate(
            "Алгоритм",
            "Объясни алгоритм по шагам, оцени сложность, приведи реализацию на Kotlin " +
                "и тесты на граничные случаи."
        ),
        PromptTemplate(
            "Regex",
            "Составь регулярное выражение по описанию. Объясни группы, приведи примеры " +
                "совпадений и несовпадений, дай код на Kotlin."
        ),
        PromptTemplate(
            "UX-текст",
            "Перепиши тексты интерфейса на понятный русский: коротко, без канцелярита, " +
                "с активными глаголами. Сохрани смысл."
        ),
        PromptTemplate(
            "Оценка задачи",
            "Оцени задачу: сложность, риски, зависимости, неизвестные и план на шаги. " +
                "Дай оптимистичную и реалистичную оценку."
        ),
        PromptTemplate(
            "Учитель",
            "Объясни тему простыми словами, затем глубже. Приведи аналогию, пример кода " +
                "и 3 вопроса для самопроверки."
        ),
    )

    private data class Stack(val name: String, val language: String, val tests: String, val hint: String)
    private data class Task(val name: String, val instruction: String)

    private val stacks: List<Stack> = """
Kotlin/Android|Kotlin|JUnit + kotlin.test|Учитывай coroutines, null-safety и Material 3.
Java/Spring|Java|JUnit 5 + Mockito|Соблюдай слои controller/service/repository.
Python|Python|pytest|Добавь type hints и запуск через venv.
JavaScript|JavaScript|Vitest|Не тяни лишние пакеты.
TypeScript|TypeScript|Vitest + tsd|Сохраняй строгую типизацию.
React|TypeScript/React|React Testing Library|Проверяй доступность и состояния загрузки.
Vue|TypeScript/Vue|Vitest + Vue Test Utils|Используй Composition API.
Svelte|TypeScript/Svelte|Vitest + Testing Library|Не ломай SSR.
Node.js|TypeScript/Node.js|node:test|Учитывай backpressure и таймауты.
Go|Go|go test|Возвращай ошибки явно, не паникуй.
Rust|Rust|cargo test|Не используй unsafe без причины.
C|C|Unity/Ceedling|Проверяй границы буферов и владение памятью.
C++|C++|GoogleTest|Соблюдай RAII и rule of zero.
C#/.NET|C#|xUnit|Используй async/await и nullable reference types.
PHP/Laravel|PHP|PHPUnit/Pest|Соблюдай PSR-12 и валидацию.
Ruby/Rails|Ruby|RSpec|Не выноси бизнес-логику в контроллеры.
Swift/iOS|Swift|XCTest|Учитывай MainActor и память.
Dart/Flutter|Dart|flutter_test|Проверяй состояния и rebuild.
Scala|Scala|ScalaTest|Предпочитай immutability.
Elixir/Phoenix|Elixir|ExUnit|Используй pattern matching и OTP.
Erlang|Erlang|EUnit|Соблюдай supervision tree.
Haskell|Haskell|Hspec|Держи функции чистыми.
Clojure|Clojure|clojure.test|Предпочитай data-first подход.
F#|F#|xUnit|Используй discriminated unions.
OCaml|OCaml|Alcotest|Не ломай типы ради короткого кода.
R|R|testthat|Проверяй NA и факторы.
Julia|Julia|Test|Учитывай типы и broadcast.
SQL/PostgreSQL|SQL|pgTAP|Проверяй план и индексы.
MySQL|SQL|mysqltest|Учитывай collation и транзакции.
MongoDB|MongoDB shell/JavaScript|mongosh assertions|Думай о схеме и индексах.
Redis|Redis commands/Lua|redis-cli checks|Следи за TTL и памятью.
Bash|Bash|bats|Ставь set -euo pipefail и кавычки.
PowerShell|PowerShell|Pester|Проверяй ошибки и -ErrorAction.
Docker|Dockerfile|hadolint + smoke test|Уменьшай слои и не запускай от root.
Kubernetes|YAML/Kubernetes|kubeconform + dry-run|Проверяй probes и resources.
Terraform|HCL/Terraform|terraform validate + plan|Не храни секреты в state.
AWS|AWS CLI/IaC|LocalStack/smoke test|Соблюдай least privilege.
GraphQL|GraphQL|graphql-js tests|Проверяй N+1 и лимиты.
Unity/C#|C#/Unity|Unity Test Framework|Следи за GC и кадровым временем.
Solidity|Solidity|Foundry|Проверяй reentrancy и overflow.
""".trimIndent().lines().filter { it.isNotBlank() }.map { line ->
        val p = line.split('|')
        Stack(p[0], p[1], p[2], p[3])
    }

    private val tasks: List<Task> = """
API-клиент|Сгенерируй типобезопасный API-клиент с обработкой ошибок, таймаутами и повторами.
CLI-утилита|Сделай CLI-утилиту с аргументами, справкой, кодами возврата и примерами запуска.
Unit-тесты|Напиши unit-тесты: успешные сценарии, границы, ошибки и регрессии.
Интеграционные тесты|Напиши интеграционный тест с реальными файлами или сетью там, где это безопасно.
Рефакторинг|Отрефактори код без изменения поведения, покажи diff и объясни риски.
Производительность|Найди узкие места, измерь их и предложи изменения по приоритету.
Безопасность|Проведи аудит безопасности и дай конкретные исправления.
Поиск багов|Найди причину бага по симптому, предложи гипотезы и минимальный патч.
Архитектура|Предложи архитектуру модулей, слои, зависимости и два trade-off варианта.
Схема БД|Спроектируй схему данных с ключами, индексами и ограничениями.
Миграции|Напиши обратимую миграцию и план безопасного развёртывания.
Dockerfile|Собери минимальный безопасный образ с кэшем зависимостей.
CI/CD|Опиши пайплайн сборки, тестов, кэша и релиза.
README|Напиши README: назначение, установка, запуск, конфиг и лицензия.
Commit message|Сгенерируй Conventional Commits по diff с телом изменения.
PR-описание|Составь PR: проблема, решение, тесты, риски и чек-лист ревью.
Code review|Проведи строгое ревью: баги, безопасность, читаемость и тесты.
Regex|Составь регулярное выражение, объясни группы и приведи примеры.
Алгоритм|Объясни алгоритм, сложность и реализуй с граничными тестами.
Парсинг|Напиши устойчивый парсер с валидацией и понятными ошибками.
Логирование|Добавь структурное логирование без секретов и лишнего шума.
Конфигурация|Сделай конфигурацию через env или файл с валидацией и значениями по умолчанию.
Обработка ошибок|Спроектируй понятные ошибки, ретраи и сообщения для пользователя.
Многопоточность|Найди гонки и предложи безопасную схему параллелизма.
Кэширование|Добавь кэш с TTL, инвалидацией и метриками попаданий.
""".trimIndent().lines().filter { it.isNotBlank() }.map { line ->
        val p = line.split('|', limit = 2)
        Task(p[0], p[1])
    }

    private val mega: List<PromptTemplate> = buildList {
        for (stack in stacks) {
            for (task in tasks) {
                add(
                    PromptTemplate(
                        title = "${stack.name} · ${task.name}",
                        prompt = "Ты senior-разработчик по ${stack.name}. ${task.instruction} " +
                            "Пиши на ${stack.language}, соблюдай идиомы и не добавляй лишних зависимостей. " +
                            "Тесты: ${stack.tests}. ${stack.hint} " +
                            "Верни готовый код, команды запуска и краткое объяснение.",
                        category = stack.name,
                    ),
                )
            }
        }
    }

    val items: List<PromptTemplate> = builtIn + mega

    fun search(query: String): List<PromptTemplate> {
        val q = query.trim()
        if (q.isEmpty()) return items
        return items.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.prompt.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true)
        }
    }
}
