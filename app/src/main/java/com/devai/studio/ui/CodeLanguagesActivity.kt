package com.devai.studio.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.devai.studio.util.ThemeManager

class CodeLanguagesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent {
            DevAITheme {
                CodeLanguagesScreen(onBack = { finish() })
            }
        }
    }
}
