package com.devai.studio.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devai.studio.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)
        onFinished()
    }

    val pulse by rememberInfiniteTransition(label = "splash").animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentAlignment = Alignment.Center,
    ) {
        DevAIBlurredGlow(
            color = scheme.primary,
            modifier = Modifier.align(Alignment.Center).scale(1.8f),
            size = 160.dp,
            blur = 64.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(112.dp).scale(pulse),
                shape = RoundedCornerShape(32.dp),
                color = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
                tonalElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(84.dp),
                    )
                }
            }
            Spacer(Modifier.size(24.dp))
            Text(
                text = "DevAI",
                color = scheme.onBackground,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Text(
                text = "STUDIO",
                color = scheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 7.sp,
            )
            Spacer(Modifier.size(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = scheme.primary,
                strokeWidth = 2.5.dp,
                trackColor = scheme.surfaceContainerHighest,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = "Material 3 · загрузка",
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "DevAI Studio 5.0",
            color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
        )
    }
}
