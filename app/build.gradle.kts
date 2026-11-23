import org.gradle.api.tasks.bundling.Zip
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun buildSecret(name: String): String =
    (localProperties.getProperty(name) ?: System.getenv(name) ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.devai.studio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devai.studio"
        minSdk = 31
        targetSdk = 34
        versionCode = 38
        versionName = "3.5.0"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "FIREBASE_API_KEY", "\"${buildSecret("FIREBASE_API_KEY")}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${buildSecret("FIREBASE_APP_ID")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${buildSecret("FIREBASE_PROJECT_ID")}\"")
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

val packLanguages by tasks.registering(Zip::class) {
    from(rootProject.file("languages"))
    archiveFileName.set("languages.zip")
    destinationDirectory.set(layout.buildDirectory.dir("generated/languagePacks/raw"))
}

android.sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/languagePacks"))

tasks.named("preBuild").configure { dependsOn(packLanguages) }

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.luaj:luaj-jse:3.0.1")
    implementation("com.google.ai.edge.litertlm:litertlm:0.0.0-alpha05")
    implementation("com.google.firebase:firebase-auth:23.2.0")

    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3:1.5.0-alpha10")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.graphics:graphics-shapes:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}