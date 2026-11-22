package com.devai.studio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.devai.studio.ui.DevAIApp
import com.devai.studio.ui.DevAITheme
import com.devai.studio.util.ThemeManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent {
            DevAITheme {
                DevAIApp()
            }
        }
    }
}
