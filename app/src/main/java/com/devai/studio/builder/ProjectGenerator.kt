package com.devai.studio.builder

/**
 * Генератор проектов «из коробки». Возвращает карту путь->содержимое,
 * которую затем упаковываем в ZIP. Готовые шаблоны не требуют интернета.
 */
object ProjectGenerator {

    fun android(
        appName: String,
        packageName: String,
        mainCode: String?,
        note: String
    ): Map<String, String> {
        val pkg = packageName.ifBlank { "com.example.app" }
        val safeName = appName.ifBlank { "MyApp" }
        val files = linkedMapOf<String, String>()
        files["settings.gradle.kts"] = androidSettings(safeName)
        files["build.gradle.kts"] = ANDROID_ROOT_BUILD
        files["gradle.properties"] = ANDROID_GRADLE_PROPS
        files["app/build.gradle.kts"] = androidAppBuild(pkg)
        files["app/src/main/AndroidManifest.xml"] = ANDROID_MANIFEST
        files["app/src/main/java/" + pkg.replace('.', '/') + "/MainActivity.kt"] =
            mainCode?.takeIf { it.isNotBlank() } ?: androidMainActivity(pkg)
        files["app/src/main/res/values/strings.xml"] = androidStrings(safeName)
        files["app/src/main/res/values/themes.xml"] = ANDROID_THEMES
        files[".github/workflows/build_apk.yml"] = BUILD_APK_WORKFLOW
        files[".gitignore"] = GITIGNORE
        files["README.md"] = note.ifBlank { "# " + safeName + "\n\nAndroid-проект (Kotlin), создан в DevAI Studio." }
        return files
    }

    fun web(title: String, html: String?): Map<String, String> {
        val files = linkedMapOf<String, String>()
        files["index.html"] = html?.takeIf { it.isNotBlank() } ?: WEB_INDEX.replace("__TITLE__", title.ifBlank { "Моё приложение" })
        files["style.css"] = WEB_CSS
        files["script.js"] = WEB_JS
        files["README.md"] = "# " + title.ifBlank { "Веб-приложение" } + "\n\nОткройте index.html в браузере."
        return files
    }

    fun python(name: String, code: String?): Map<String, String> {
        val files = linkedMapOf<String, String>()
        files["main.py"] = code?.takeIf { it.isNotBlank() } ?: PY_MAIN
        files["requirements.txt"] = "# Зависимости (pip install -r requirements.txt)\n"
        files["README.md"] = "# " + name.ifBlank { "Python-скрипт" } + "\n\nЗапуск: python main.py"
        return files
    }

    fun kotlinCli(name: String, code: String?): Map<String, String> {
        val files = linkedMapOf<String, String>()
        files["settings.gradle.kts"] = "rootProject.name = \"" + name.ifBlank { "cli" }.replace("\"", "") + "\"\n"
        files["build.gradle.kts"] = KOTLIN_CLI_BUILD
        files["src/main/kotlin/Main.kt"] = code?.takeIf { it.isNotBlank() } ?: KOTLIN_CLI_MAIN
        files["README.md"] = "# " + name.ifBlank { "Kotlin CLI" } + "\n\nЗапуск: gradle run"
        return files
    }

    private fun androidSettings(name: String): String =
        "pluginManagement {\n    repositories {\n        google()\n        mavenCentral()\n        gradlePluginPortal()\n    }\n}\n" +
        "dependencyResolutionManagement {\n    repositories {\n        google()\n        mavenCentral()\n    }\n}\n" +
        "rootProject.name = \"" + name.replace("\"", "") + "\"\ninclude(\":app\")\n"

    private fun androidAppBuild(pkg: String): String = """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "__PKG__"
    compileSdk = 34
    defaultConfig {
        applicationId = "__PKG__"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
""".trimIndent().replace("__PKG__", pkg)

    private fun androidMainActivity(pkg: String): String = """
package __PKG__

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Hello from DevAI Studio"
        tv.textSize = 22f
        tv.setPadding(48, 96, 48, 48)
        setContentView(tv)
    }
}
""".trimIndent().replace("__PKG__", pkg)

    private fun androidStrings(name: String): String = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">__NAME__</string>
</resources>
""".trimIndent().replace("__NAME__", name.replace("&", "&amp;").replace("<", "&lt;"))

    private const val ANDROID_ROOT_BUILD = """
plugins {
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
"""

    private const val ANDROID_GRADLE_PROPS = """
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
"""

    private const val ANDROID_MANIFEST = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.App">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""

    private const val ANDROID_THEMES = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
"""

    private const val BUILD_APK_WORKFLOW = """
name: Build APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
      - name: Generate Gradle wrapper
        run: gradle wrapper --gradle-version 8.6
      - name: Build debug APK
        run: ./gradlew assembleDebug --no-daemon
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
"""

    private const val GITIGNORE = """
.gradle/
build/
local.properties
.idea/
*.iml
"""

    private const val WEB_INDEX = """
<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>__TITLE__</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <main class="card">
    <h1>__TITLE__</h1>
    <p>Приложение создано в DevAI Studio.</p>
    <button id="btn">Нажми меня</button>
    <p id="out"></p>
  </main>
  <script src="script.js"></script>
</body>
</html>
"""

    private const val WEB_CSS = """
* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  font-family: system-ui, sans-serif;
  background: linear-gradient(135deg, #6750A4, #4F378B);
}
.card {
  background: #fff;
  padding: 32px;
  border-radius: 18px;
  box-shadow: 0 10px 40px rgba(0,0,0,.25);
  text-align: center;
  max-width: 90vw;
}
button {
  background: #6750A4;
  color: #fff;
  border: 0;
  border-radius: 24px;
  padding: 12px 28px;
  font-size: 16px;
  cursor: pointer;
}
button:hover { background: #4F378B; }
"""

    private const val WEB_JS = """
const btn = document.getElementById('btn');
const out = document.getElementById('out');
let count = 0;
btn.addEventListener('click', () => {
  count += 1;
  out.textContent = 'Нажатий: ' + count;
});
"""

    private const val PY_MAIN = """
def main():
    print("Hello from DevAI Studio!")

if __name__ == "__main__":
    main()
"""

    private const val KOTLIN_CLI_BUILD = """
plugins {
    kotlin("jvm") version "1.9.24"
    application
}
repositories {
    mavenCentral()
}
application {
    mainClass.set("MainKt")
}
"""

    private const val KOTLIN_CLI_MAIN = """
fun main() {
    println("Hello from DevAI Studio!")
}
"""
}
