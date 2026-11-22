# AGENTS.md — DevAI Studio

## Build & release
- JDK 21: `/usr/lib/jvm/java-21-openjdk-amd64`
- Android SDK: `/workspace/android-sdk`
- Gradle 8.9: `/workspace/gradle-8.9/bin/gradle` (wrapper updated to 8.9)
- Kotlin 2.2.20, AGP 8.6.0, compileSdk 35, targetSdk 34, minSdk 31 (LiteRT-LM requires it), arm64-v8a only.
- Release: `./gradlew :app:assembleRelease`, then sign with `apksigner` using `/workspace/keys/devai-release.jks` (alias `devai`, password in `pass.txt`).

## Offline models (v2.3)
- Runtime: `com.google.ai.edge.litertlm:litertlm:0.0.0-alpha05`.
- Catalog/store: `data/LocalModelStore.kt`; downloader: `data/ModelDownloader.kt`; UI: `ui/LocalModelsActivity.kt`.
- Models are downloaded to `getExternalFilesDir("models")`; `.part` files support resume.
- Free: SmolLM 135M, Qwen2.5 0.5B. Premium: TinyLlama 1.1B, Qwen2.5 1.5B.

## Themes & Premium
- `util/ThemeManager.kt`: M3 (free, dynamic color), Expressive/Retro (premium).
- `data/PremiumStore.kt`: local entitlement and promo codes (client-side validation; use a server for production).
- Premium UI: `ui/PremiumActivity.kt`; gates: themes, large models, chat export.

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
- Google Play Billing is a placeholder (`ui/PremiumActivity.kt` "Купить" dialog); activation is promo-based.
- Do not commit `build/`, `local.properties`, keystore.

## Tools & plugins (v2.11.3)
- New bottom-nav tab `ui/ToolsScreen.kt`: multi-source search, TTS, MCP, characters, `.devai` plugins, QR export, plugin docs.
- Search (`data/SearchClient.kt`) queries Tavily (optional key), GitHub (repos/issues; optional token also enables code search), Stack Overflow, Wikipedia, DuckDuckGo and Hacker News in parallel. Tavily/GitHub keys are optional because the other sources work without auth.
- `.devai` plugins are real Lua 5.2 plugins via `org.luaj:luaj-jse:3.0.1` (MIT). Engine: `data/LuaPluginEngine.kt`; store/parser: `data/PluginStore.kt`.
- Sandbox loads only base/table/string/math; `io`, `os`, `package`, `require`, `load*`, `dofile`, `debug`, `luajava` are removed.
- Plugin commands are user-invoked; plugin tools are exposed to the AI through OpenAI-compatible tool calling (`ai/ToolAgent.kt`, `ai/OpenAiCompatClient.kt`). MCP tools use the same `AgentTool` abstraction.
- Docs: `docs/PLUGINS.md` (large guide) copied to `assets/devai-plugins.md`; in-app renderer/download in `ui/PluginDocsActivity.kt`.
- Samples: `assets/plugins/{summarizer,code-review,lua-tools}.devai`.
- Chat/Tools/Providers/Settings chips and actions use Material icons; emoji are not used as UI icons.
- App icon: adaptive `drawable/ic_launcher_{background,foreground,monochrome}.xml` (gradient D + spark), v33 monochrome; README image `docs/devai-icon.png`.
- Gemini input is cleaned by `ai/PromptSanitizer.kt`; it strips `<bos>`, `<start_of_turn>`, ChatML and similar control tokens before sending.
- Keep Vexel untouched: verify `git diff v2.9.0 -- app/src/main/kotlin/com/qwenz/qwel app/src/main/assets/{icons,fonts,fun,lang}` is empty on every release.
