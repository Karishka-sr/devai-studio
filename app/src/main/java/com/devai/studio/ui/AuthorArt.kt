package com.devai.studio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import com.devai.studio.R

@Composable
fun AuthorArtBanner(modifier: Modifier = Modifier) {
    MaterialTheme(colorScheme = darkColorScheme(onSurface = Color(0xFFEAF7E6))) {
        BoxWithConstraints(
            modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0D4F1D))
        ) {
            val scale = minOf(maxWidth, maxHeight) / 512.dp
            Box(
                Modifier
                    .size(512.dp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                VexelArt()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VexelArt() {
    val uriHandler = LocalUriHandler.current
    Box(
        Modifier
            .size(512.dp, 512.dp)
            .background(Color(0xFF0D4F1D))
    ) {
        Box(
            Modifier
                .offset(-163.27.dp, -99.07.dp)
                .size(255.21.dp, 255.21.dp)
                .background(Color(0xFF305E0C), MaterialShapes.Cookie9Sided.toShape()),
        )
        Box(
            Modifier
                .offset(49.17.dp, 38.07.dp)
                .size(413.67.dp, 413.67.dp)
                .rotate(2.71f)
                .alpha(0.98f)
                .background(Color(0xFF3E7E0C), MaterialShapes.SoftBurst.toShape()),
        )
        Box(
            Modifier
                .offset(86.9.dp, 75.81.dp)
                .size(338.2.dp, 338.2.dp)
                .rotate(3.6f)
                .background(Color(0xFF2A5409), MaterialShapes.SoftBurst.toShape()),
        )
        Box(
            Modifier
                .offset(-14.65.dp, 382.47.dp)
                .size(213.17.dp, 213.17.dp)
                .background(Color(0xFF305E0C), MaterialShapes.Pentagon.toShape()),
        )
        Box(
            Modifier
                .offset(329.18.dp, 28.54.dp)
                .size(151.96.dp, 151.96.dp)
                .background(Color(0xFF213C08), RoundedPolygon.star(5).toShape()),
        )
        Box(
            Modifier
                .offset(-35.66.dp, 448.75.dp)
                .size(163.15.dp, 163.15.dp)
                .background(Color(0xFF3E7E0C), MaterialShapes.SoftBoom.toShape()),
        )
        Text(
            text = "t.me/FemClaudeMrrr",
            modifier = Modifier
                .offset(0.dp, 466.11.dp)
                .alpha(0.4f)
                .clickable(onClick = { uriHandler.openUri("https://t.me/FemClaudeMrrr") })
                .blur(1.07.dp, BlurredEdgeTreatment.Unbounded),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 36.71.sp,
            fontWeight = FontWeight.W700,
            softWrap = false,
        )
        Box(
            Modifier
                .offset(371.2.dp, 351.1.dp)
                .size(281.5.dp, 281.5.dp)
                .background(Color(0xFF10802B), MaterialShapes.VerySunny.toShape()),
        )
        Text(
            text = "Miko",
            modifier = Modifier.offset(169.2.dp, 208.4.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 76.09.sp,
            fontWeight = FontWeight.W700,
            softWrap = false,
        )
        Image(
            painter = painterResource(R.drawable.ic_android_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .offset(174.8.dp, 165.64.dp)
                .size(94.35.dp, 55.78.dp),
        )
    }
}
