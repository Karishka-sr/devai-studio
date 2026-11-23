package com.devai.studio

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.devai.studio.data.CodeLanguages
import com.devai.studio.ui.DevAIApp
import com.devai.studio.ui.DevAITheme
import com.devai.studio.ui.SplashScreen
import com.devai.studio.util.ThemeManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        CodeLanguages.load(this)
        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            DevAITheme {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(450),
                    label = "splash",
                ) { splash ->
                    if (splash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        DevAIApp()
                    }
                }
            }
        }
    }
}
