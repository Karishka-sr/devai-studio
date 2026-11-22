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
- `LocalPrompt` always appends a Russian-language instruction, and `AppPrefs.DEFAULT_SYSTEM_PROMPT` / `DEV_SYSTEM_PROMPT` require Russian answers. This fixes small local models drifting into English benchmark-style output.
- `ai/ResponseGuard.kt` answers simple greetings without inference, filters leaked benchmark/task text from streamed local responses, and drops such assistant turns from prompt history.
- `data/ModelDownloader.kt` retries, resumes, and falls back to `hf-mirror.com` for Hugging Face URLs.

## Themes & Premium (v2.14.2)
- `util/ThemeManager.kt`: M3 (free, dynamic color), Expressive/Retro (premium).
- `data/PremiumPlan.kt`: `PremiumPlan` (FREE/WEEK/MONTH/YEAR/LIFETIME) and 59 `PremiumFeature` entries with `minPlan`.
- `data/PremiumStore.kt`: local entitlement, expiry timestamps, tier-aware codes (`DEVAI-WEEK`, `DEVAI-MONTH`, `DEVAI-YEAR`, `DEVAI-LIFETIME`), legacy codes migrate to LIFETIME.
- `data/TelegramStars.kt`: order text and `t.me/FemClaudeMrrr` payment link; codes are issued manually after Stars payment.
- Premium UI: `ui/PremiumActivity.kt` (tier cards, feature matrix, activation, Stars buttons). Gates: themes, model tiers, chat export, search/MCP/plugins/TTS/characters, chat customization, prompt library, backups, QR, history limit.
- Chat backups: `ChatSessionsStore.exportJson/importJson`; prompt library: `data/PromptLibrary.kt` (24 prompts).

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
- Settings → «Вид» → «Кастомизация»: accent palette, chat font family, bubble width, avatars, compact mode; «Чат» also has code font size, haptics and delete confirmation.
- Keep Vexel untouched: verify `git diff v2.9.0 -- app/src/main/kotlin/com/qwenz/qwel app/src/main/assets/{icons,fonts,fun,lang}` is empty on every release.

## PC version (v2.12.0)
- Module `desktop/`: Kotlin JVM 1.8 + Swing, main `com.devai.studio.desktop.MainKt`.
- Build jar: `./gradlew :desktop:fatJar` -> `desktop/build/libs/DevAIStudio-all.jar`.
- Windows bundle: JRE 8u512 x64, stage `app/`, `runtime/`, `DevAI-Android.apk`, `icon.ico`.
- Separate PC repository: https://github.com/Karishka-sr/devai-studio-pc
- Installer: `desktop/installer.nsi` (NSIS), build with `makensis installer.nsi` from the stage directory. It selects Windows 7/10/11, writes `target.ini`, and has a custom components page (full/minimal).
- Full components: Vexel APK, ADB platform-tools, desktop/Start-menu shortcuts, `Vexel.cmd` launcher.
- The app resolves the bundled APK, ADB, and `target.ini` from the JAR's parent directory, so shortcuts do not depend on `user.dir`.
