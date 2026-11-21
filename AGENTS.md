# AGENTS.md — DevAI Studio

## Build & release
- JDK 21: `/usr/lib/jvm/java-21-openjdk-amd64`
- Android SDK: `/workspace/android-sdk`
- Gradle 8.9: `/workspace/gradle-8.9/bin/gradle` (wrapper updated to 8.9)
- Kotlin 2.1.0, AGP 8.6.0, compileSdk 35, targetSdk 34, minSdk 31 (LiteRT-LM requires it), arm64-v8a only.
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

## Compose UI (v2.3.2)
- Compose BOM 2025.10.01 (UI 1.9.4, Material3 1.4.0), `androidx.graphics:graphics-shapes:1.0.1`, Compose compiler plugin 2.1.0.
- Author banner: `ui/AuthorArt.kt`, embedded via `ComposeView` (`author_art`) in `fragment_settings.xml`.
- Exact Material3 expressive shapes are ported from Material3 1.5.0 (`cookie9`, `softBurst`, `pentagon`, `softBoom`, `verySunny`). Material3 1.5.0 itself requires AGP 9.1 / API 37 — do not bump without upgrading the whole toolchain.
- Android logo: `res/drawable/ic_android_logo.xml`.

## Notes
- Google Play Billing is a placeholder (`ui/PremiumActivity.kt` "Купить" dialog); activation is promo-based.
- Do not commit `build/`, `local.properties`, keystore.
