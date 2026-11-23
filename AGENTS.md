# AGENTS.md — DevAI Studio

## Build & release
- JDK 21: `/workspace/jdk21`
- Android SDK: `/workspace/android-sdk`
- Gradle 8.9: `/workspace/gradle-8.9/bin/gradle` (wrapper updated to 8.9)
- Kotlin 2.2.20, AGP 8.6.0, compileSdk 35, targetSdk 34, minSdk 31 (LiteRT-LM requires it), arm64-v8a only.
- Release: `./gradlew :app:assembleRelease`, then sign with `apksigner` using `/workspace/keys/devai-release.jks` (alias `devai`, password in `pass.txt`).

## Offline models (v2.3)
- Runtime: `com.google.ai.edge.litertlm:litertlm:0.0.0-alpha05`.
- Catalog/store: `data/LocalModelStore.kt`; downloader: `data/ModelDownloader.kt`; UI: `ui/LocalModelsActivity.kt`.
- Models are downloaded to `getExternalFilesDir("models")`; `.part` files support resume.
- Free: SmolLM 135M, LFM2.5 350M, SmolLM2 360M, Granite 4.0 H 350M, Qwen2.5 0.5B, Qwen3 0.6B.
- Premium Неделя: LFM2.5 1.2B, TinyLlama 1.1B, Qwen2.5 1.5B. Месяц: Qwen2.5 Coder 1.5B, Qwen3 1.7B. Год: DeepSeek R1 1.5B, SmolLM3 3B, Phi-4 Mini.
- Catalog uses public LiteRT-LM files (`.task` and `.litertlm`). Do not add gated Hugging Face repos (Gemma, Falcon-H1): direct downloads return 401 without a token.
- LiteRT-LM applies the model chat template itself. `ai/LocalPrompt.kt` sends plain text and sanitizes every message; never send `<bos>`/ChatML/Gemma control tokens to `generateContentStream`.
- `LocalPrompt` always appends a Russian-language instruction and now enforces a character budget derived from `AppPrefs.localContextTokens` (512–1280 tokens). It keeps the last user turn and prioritises the search-results section; never remove that budget or small models will drop the question or sources. `AppPrefs.DEFAULT_SYSTEM_PROMPT` / `DEV_SYSTEM_PROMPT` default to Russian answers.
- `ai/ResponseGuard.kt` answers simple greetings without inference, filters leaked benchmark/task text and degenerate repetition from local responses, and drops such assistant turns from prompt history.
- `data/ModelDownloader.kt` retries, resumes, and falls back to `hf-mirror.com` for Hugging Face URLs.

## Themes & Premium (v3.4.0)
- `ui/DevAIDesign.kt` is the shared Aurora design system: gradient background, logo/top bar, rounded bordered cards, icon tiles, status pills, gradient buttons. Use these composables instead of raw `Card`/`TopAppBar` on new screens.
- Contrast is enforced at the container level: `DevAIBackground` provides `LocalContentColor = onBackground`; `DevAICard`, top-bar buttons, bottom bar, chat bubbles and custom `Surface`s set an explicit `contentColor`. Do not rely on `contentColorFor()` for translucent colors (`surface.copy(alpha = ...)`) — it returns `Unspecified` and can inherit an unreadable color.
- `ui/DevAIApp.kt` owns the palette, typography, shapes, and the floating rounded bottom bar. Default M3 is now Aurora (indigo/mint/coral); Expressive/Retro remain premium themes.
- `res/values/themes.xml` sets `android:forceDarkAllowed=false` and explicit `colorOnSurface`, `colorOnSurfaceVariant`, `colorOnBackground`, `colorOutline*`, `colorOnSecondary`/`colorOnTertiary`. Keep these explicit: the XML `Theme.DevAIStudio` is DayNight but its surface palette is intentionally light.
- Settings is a two-level Claude/ChatGPT-style screen (`ui/SettingsScreen.kt`): `SettingsHome` has a profile card, search field and grouped navigation rows; each category opens a detail page with a back arrow and `SettingsSection` cards. Add new settings to a category, not to the home list.
- Theme/accent/font changes call `DevAIThemeState.refresh()` instead of `activity.recreate()`. `DevAITheme` reads `DevAIThemeState.revision`, so colors update in place and the selected tab/category is never lost. Do not reintroduce `recreate()` for settings changes.
- Settings categories: Основное (Генерация, Модели, Чат), Интерфейс (Вид), Аккаунт (Premium, Данные), Приложение (О приложении). «Чат» is split into Сообщения / Поведение / Отклик; «Данные» into История / Резервные копии / Сброс.
- `util/ThemeManager.kt`: M3/Aurora (free, dynamic color), Expressive/Retro (premium).
- `data/PremiumPlan.kt`: `PremiumPlan` (FREE/WEEK/MONTH/YEAR/LIFETIME) and 59 `PremiumFeature` entries with `minPlan`.
- `data/PremiumStore.kt`: local entitlement, expiry timestamps, tier-aware codes (`DEVAI-WEEK`, `DEVAI-MONTH`, `DEVAI-YEAR`, `DEVAI-LIFETIME`), legacy codes migrate to LIFETIME.
- `data/TelegramStars.kt`: order text and `t.me/FemClaudeMrrr` payment link; codes are issued manually after Stars payment.
- Premium UI: `ui/PremiumActivity.kt` (tier cards, feature matrix, activation, Stars buttons). Gates: themes, model tiers, chat export, search/MCP/plugins/TTS/characters, chat customization, prompt library, backups, QR, history limit.
- Chat backups: `ChatSessionsStore.exportJson/importJson`; prompt library: `data/PromptLibrary.kt` (1024 prompts, searchable).

## Profile & Phone Auth (3.5.1)
- `ProfileStore` keeps nickname/phone/uid in EncryptedSharedPreferences (fallback prefs).
- `PhoneAuthManager` uses Firebase Phone Auth. Configure `FIREBASE_API_KEY`, `FIREBASE_APP_ID`, `FIREBASE_PROJECT_ID` in `local.properties`; without them the profile UI is disabled with an explicit message. See `docs/PHONE_AUTH.md`.
- Settings category `PROFILE` edits nickname (2–32 chars) and runs SMS code request/confirm; `PhoneNumbers` normalizes RU formats and is unit-tested.

## DevAI Core (3.6.1 → 3.6.4)
- `ai/DevAICore.kt` is the custom assistant layer: local intent replies (memory, prompt search, profile/SMS/language/model help, status, time), `SYSTEM_PROMPT`, and `offlineReply`.
- `ChatMode.CORE` is the third chat mode. `ChatScreen.send` runs `DevAICore.localReply` before provider config; if no provider/key is configured it falls back to `offlineReply` instead of failing.
- `AppPrefs.systemPromptForMode(CORE)` returns `DevAICore.SYSTEM_PROMPT`; custom user prompt still wins.
- `DevAICore.findPrompts` first uses `PromptLibrary.search`, then falls back to all-term matching with simple stemming for phrases like «Kotlin API» and «тестов».
- Since Core 0.2, `ai/AiQuality.kt` is the shared answer policy: core/default/dev prompts plus `ANSWER_RULES` and `SEARCH_RULES`. `ChatScreen` appends the rules after any custom prompt, so user prompts cannot silently disable fact-checking/source rules.
- `data/SearchQueryPlanner.kt` strips greetings/polite wrappers/question prefixes, detects fresh/code intents and produces alternate queries (`... 2026`, `... stackoverflow`). `SearchClient` runs alternates only when the first pass is thin, prioritises sections by intent, caps each section and deduplicates URLs. Keep planner logic pure and unit-tested.
- Tests: `DevAICoreTest` checks identity and prompt search; `SearchQueryPlannerTest`, `LocalPromptTest`, `ResponseGuardTest`, `SearchClientTest` cover the query, budget and guard paths. Keep local skill replies deterministic and offline.
- Since 3.6.2, `ChatScreen` enables web search automatically in `ChatMode.CORE` and for the `devai-core` provider. The chip shows «Интернет · авто»; a placeholder «Ищу в интернете…» appears while sources load.
- Since 3.6.3, `SearchClient.isSearchWorthy` blocks web search for greetings and small talk («Привет», «Спасибо», «Кто ты», «Что ты умеешь», «Как дела»). `ChatScreen` applies it to both automatic and manual search, so a greeting gets a normal answer without a «Источники» block.
- Since 3.6.4, update notifications use a full-screen Compose `Dialog` overlay from the Vexel design (`UpdateOverlay.kt`), with «Скачать» and «Позже» actions. The old top banner was removed. The 360×800 Vexel canvas is scaled down to fit small screens; Vexel itself is untouched.
- `data/SearchClient.kt` aggregates Tavily (optional key), Bing (English), Google News RSS (news queries), CBR daily rates (currency queries), wttr.in (weather queries), GitHub, Stack Overflow, Wikipedia (with lead extract), DuckDuckGo and Hacker News. Cyrillic queries skip Bing because its mobile/RU HTML is unreliable; Wikipedia + Google News cover them.
- Search results are injected into the system prompt with an instruction to cite sources; `withSearchSources` appends up to five links to the final answer. The prompt also carries the current date/time.
- `data/UpdateChecker.kt` checks the latest GitHub release for `Karishka-sr/devai-studio` and `devai-studio-pc`, using the Releases API and falling back to `releases.atom` + `expanded_assets` when the API is rate-limited. It only accepts releases with an `.apk` asset and compares semantic versions against `BuildConfig.VERSION_NAME`.
- Live updates: `DevAIApp` polls while the activity is `RESUMED` — every 30 s through the cheap Pages `docs/data/latest.json`, every 5 min and on each resume through GitHub. The site JSON is the fast path; GitHub is the authority. `UpdateChecker` stores the dismissed version, so a dismissed release stays hidden but a newer one reopens the overlay. Do not turn this back into a one-shot launch check or a global dismissed boolean.
- `DevAIApp` shows `UpdateOverlay` (`ui/UpdateOverlay.kt`) when a newer release exists: a full-screen Compose `Dialog` with «Скачать» and «Позже», scaled from the 360×800 Vexel canvas. Positive results are cached in `devai_update` prefs; negative checks are throttled to 15 minutes.
- Tests: `SearchClientTest` covers desktop/mobile Bing markup and empty results; `UpdateCheckerTest` covers version ordering.

## DevAI Core API & website (v0.2)
- `docs/index.html` is the product landing page (GitHub Pages root): hero, features, Vexel, local models, install steps, FAQ and live release panel. `docs/download.html` is the «Скачать с сайта» resolver; it fetches the latest release and redirects to its APK.
- `docs/site.js` reads `docs/data/latest.json` and falls back to the GitHub Releases API. It updates `[data-version]`, `[data-apk-size]`, `[data-release-notes]`, `[data-apk-sha]` and all download links. Do not hardcode a version in HTML when a data attribute is available.
- `scripts/update_site_release.py` writes `docs/data/latest.json` from the latest GitHub release. `.github/workflows/site-release.yml` runs it daily, on `release` and manually; the build workflow ignores `docs/**` and `scripts/**`.
- `docs/api.html` is the DevAI Core API key page (previously `docs/index.html`). It keeps `docs/app.js` and `docs/styles.css`; the product site uses `docs/site.css` and `docs/site.js`. Keep the API page linked from the header/footer.
- `server/devai_server.py` is an OpenAI-compatible stdlib server: `/v1/chat/completions` (SSE), `/v1/models`, `/health`, admin key CRUD.
- Public signup: `POST /v1/keys/challenge` returns a proof-of-work challenge; `POST /v1/keys/register` verifies it, binds to the hashed client IP, and creates a key. Anti-bot: difficulty 4, 1.2s minimum age, honeypot `website`, 8 open challenges/IP, 3 keys/IP/day, 300/day global. Never expose the admin token in the static site.
- GitHub Pages publishes `docs/` at `https://karishka-sr.github.io/devai-studio/`. The API page is `https://karishka-sr.github.io/devai-studio/api.html`; the API base is stored in `localStorage` and defaults to the current work host. Update `DEFAULT_API` in `docs/app.js` when the server moves.
- Android `ProviderCatalog` has a `devai-core` provider (`devai-core-0.1`, OpenAI-compatible). Its `baseUrl` points to the current work host and must be updated together with `DEFAULT_API`. The key dialog has an explicit clipboard paste button.
- Tests: `python3 -m py_compile server/devai_server.py`; public flow is verified with challenge → SHA-256 nonce → register → `/v1/models`.

## Mega Settings (3.4.0)
- `SettingsScreen` home has a live summary card, 5 one-tap generation presets, 8 quick toggles (dynamic color, haptics, code wrap, send-on-enter), popular query chips, and keyword search across `SettingsCategory.keywords`.
- `SettingsHome` receives `SettingsSummary`, `QuickToggles`, `onPreset`, `onToggle`; `applyQuickPreset`/`toggleQuick` persist through `AppPrefs`.

## Mega Prompt Library (3.4.0)
- `data/PromptLibrary.kt` builds 1024 prompts: 24 built-in + 40 stacks × 25 tasks. `PromptLibrary.search(query)` filters by title/prompt/category; both chat sheet and settings dialog have search. `PromptLibraryTest` asserts >=1000 unique prompts.
## Feature Pack 2.17.0 → 3.4.0
- `data/QuickTools.kt` contains 56 local utilities (Base64, URL, JSON/CSV, JWT, hashes, text, regex, diff, numbers, time, colors, generators, MIME/HTTP reference). Add new tools there, not as ad-hoc UI code.
- `ui/ExtraToolsScreen.kt` is the generic workbench: search, category chips, one input + optional second input, output, copy and share. `ExtraToolsActivity` hosts it from «Ещё → Лаборатория».
- `ui/ChatAiActions.kt` holds three sheets: AI actions (14 prompt-based operations), prompt library, and chat search. Chat exposes them as horizontally scrolling chips.
- `data/MemoryStore.kt` stores local facts. Chat injects them into the system prompt; Settings → Чат → Память manages them.
- `AppPrefs.responseStyle` / `responseLanguage` append style and language rules to the system prompt. Settings → Чат exposes both.
- Chat also supports temporary (non-persisted) sessions, voice input via `RecognizerIntent`, per-message copy/share, and a prompt-library shortcut.
- `QuickToolsTest` exercises every tool with a valid sample and asserts the catalog stays at 50+ entries. It uses the real `org.json` test dependency because Android's stub throws in JVM tests.
- `languages/*.json` is the source of truth for 75 languages (Kotlin default). The build packs them into `res/raw/languages.zip`; nothing is stored in `assets`.
- `CodeLanguages.load(ctx)` seeds `filesDir/languages` and `Android/data/com.devai.studio/files/languages` from that zip, then reads the files. Users can add JSON there without rebuilding; the hardcoded list is only a fallback. Add a language by adding one JSON file with `id/name/short/extension/comment/testFramework/description/example/order`. `data/CodeLanguages.kt` provides aliases and `fromFileName`. The chat injects `CodeLanguages.instruction` into the DEV system prompt; `CodeLanguagesActivity` is the picker; `ChatAiActions` uses the selected language for test generation.
- `data/CodeSyntax.kt` is the offline lexer for 50 languages: keywords, types, strings, comments, numbers, annotations, functions and markup. `ui/CodeHighlight.kt` renders it in chat code blocks and language previews. Aliases (`js`, `ts`, `c++`, `c#`, `objective-c`) and auto-detection for untagged fences are supported.
- `ui/ChatWallpapers.kt` lists 24 offline wallpapers; `WallpaperActivity`/`WallpaperScreen` is the picker. Full images live in `res/drawable-nodpi/chat_wall_XX.png`, thumbnails in `chat_wall_thumb_XX.png`. Chat reads `AppPrefs.chatWallpaper`/`chatWallpaperDim` and renders the selected image behind the message list.

## Compose UI (v2.4.0)
- Compose BOM 2026.03.01 (UI/foundation 1.10.6, Material3 1.4.0), `androidx.activity:activity-compose:1.10.1`, `androidx.graphics:graphics-shapes:1.1.0`, Compose compiler plugin 2.2.20.
- Author banner: `ui/AuthorArt.kt`, embedded via `ComposeView` (`author_art`) in `fragment_settings.xml`.
- Exact Material3 expressive shapes are ported from Material3 1.5.0 (`cookie9`, `softBurst`, `pentagon`, `softBoom`, `verySunny`). Material3 1.5.0 itself requires AGP 9.1 / API 37 — do not bump without upgrading the whole toolchain.
- Android logo: `res/drawable/ic_android_logo.xml`.

## Vexel / Qwel constructor (v2.4.0)
- Source: `app/src/main/kotlin/com/qwenz/qwel` (package `com.qwenz.qwel`), assets in `app/src/main/assets/{fonts,fun,lang}`.
- Launched full-screen from `nav_builder`: `MainActivity` starts `com.qwenz.qwel.MainActivity` and returns `false`, so the previous tab stays selected when returning.
- Old builder removed: `builder/`, `data/ProjectStore.kt`, `ui/ProjectAdapter.kt`, `ui/WorkspaceFragment.kt`, `fragment_builder.xml`, `fragment_workspace.xml`, `item_project.xml`, `nav_projects`.
- Live blur/glass is patched out: public Compose 1.10.6 has no `GraphicsLayer`. `ui/common/Glass.kt` and `render/Backdrop.kt` are no-ops; PNG/SVG blur still works.
- Credit: Vexel by @cheburek_dev (https://t.me/cheburek_dev), used with permission.

## Notes
- Google Play Billing is not used. Premium is activated by tier code after Telegram Stars payment; entitlement is client-side.
- Do not commit `build/`, `local.properties`, keystore.

## Tools & plugins (v2.13.0)
- New bottom-nav tab `ui/ToolsScreen.kt`: multi-source search, TTS, MCP, characters, `.devai` plugins, QR export, plugin docs.
- Search (`data/SearchClient.kt`) queries Tavily (optional key), GitHub (repos/issues; optional token also enables code search), Stack Overflow, Wikipedia, DuckDuckGo and Hacker News in parallel. Tavily/GitHub keys are optional because the other sources work without auth.
- `.devai` plugins are real Lua 5.2 plugins via `org.luaj:luaj-jse:3.0.1` (MIT). Engine: `data/LuaPluginEngine.kt`; store/parser: `data/PluginStore.kt`.
- Sandbox loads only base/table/string/math; `io`, `os`, `package`, `require`, `load*`, `dofile`, `debug`, `luajava` are removed.
- Plugin commands are user-invoked; plugin tools are exposed to the AI through OpenAI-compatible tool calling (`ai/ToolAgent.kt`, `ai/OpenAiCompatClient.kt`). MCP tools use the same `AgentTool` abstraction.
- Docs: `docs/PLUGINS.md` (large guide) copied to `assets/devai-plugins.md`; in-app renderer/download in `ui/PluginDocsActivity.kt`.
- Samples: `assets/plugins/{summarizer,code-review,lua-tools}.devai`.
- Chat/Tools/Providers/Settings chips and actions use Material icons; emoji are not used as UI icons.
- App icon: adaptive `drawable/ic_launcher_{background,foreground,monochrome}.xml` (gradient D + spark), v33 monochrome; README image `docs/devai-icon.png`.
- Prompts are cleaned by `ai/PromptSanitizer.kt`; it strips `<bos>`, `<start_of_turn>`, ChatML and similar control tokens before Gemini and LiteRT-LM calls.
- Settings → «Вид»: theme previews (Aurora/Expressive/Retro), dynamic color, accent palette, chat font family, bubble width, avatars, compact mode; «Чат» also has code font size, haptics and delete confirmation.
- Keep Vexel untouched: verify `git diff v2.9.0 -- app/src/main/kotlin/com/qwenz/qwel app/src/main/assets/{icons,fonts,fun,lang}` is empty on every release.

## PC version (v3.2.1)
- Module `desktop/`: Kotlin JVM 1.8 + Swing, main `com.devai.studio.desktop.MainKt`.
- PC language selector uses `desktop/.../CodeLanguages.kt` (same 50 languages); `AppSettings.codeLanguage` persists in `settings.properties` and is appended to the system prompt.
- Build jar: `./gradlew :desktop:fatJar` -> `desktop/build/libs/DevAIStudio-all.jar`.
- Windows bundle: JRE 8u512 x64, stage `app/`, `runtime/`, `DevAI-Android.apk`, `icon.ico`.
- Separate PC repository: https://github.com/Karishka-sr/devai-studio-pc
- Installer: `desktop/installer.nsi` (NSIS), build with `makensis installer.nsi` from the stage directory. It selects Windows 7/10/11, writes `target.ini`, and has a custom components page (full/minimal).
- Full components: Vexel APK, ADB platform-tools, desktop/Start-menu shortcuts, `Vexel.cmd` launcher.
- The app resolves the bundled APK, ADB, and `target.ini` from the JAR's parent directory, so shortcuts do not depend on `user.dir`.
