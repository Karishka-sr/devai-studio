# AGENTS.md — DevAI Studio

## Build & release
- JDK 21: `/usr/lib/jvm/java-21-openjdk-amd64`
- Android SDK: `/workspace/android-sdk`
- Gradle 8.6: `/workspace/gradle-8.6/bin/gradle` (or `./gradlew`)
- Kotlin 2.1.0, AGP 8.4.0, minSdk 31 (LiteRT-LM requires it), arm64-v8a only.
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

## Notes
- Google Play Billing is a placeholder (`ui/PremiumActivity.kt` "Купить" dialog); activation is promo-based.
- Do not commit `build/`, `local.properties`, keystore.
