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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import com.devai.studio.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
                .background(Color(0xFF305E0C), Cookie9Shape),
        )
        Box(
            Modifier
                .offset(49.17.dp, 38.07.dp)
                .size(413.67.dp, 413.67.dp)
                .rotate(2.71f)
                .alpha(0.98f)
                .background(Color(0xFF3E7E0C), SoftBurstShape),
        )
        Box(
            Modifier
                .offset(86.9.dp, 75.81.dp)
                .size(338.2.dp, 338.2.dp)
                .rotate(3.6f)
                .background(Color(0xFF2A5409), SoftBurstShape),
        )
        Box(
            Modifier
                .offset(-14.65.dp, 382.47.dp)
                .size(213.17.dp, 213.17.dp)
                .background(Color(0xFF305E0C), PentagonShape),
        )
        Box(
            Modifier
                .offset(329.18.dp, 28.54.dp)
                .size(151.96.dp, 151.96.dp)
                .background(Color(0xFF213C08), Star5Shape),
        )
        Box(
            Modifier
                .offset(-35.66.dp, 448.75.dp)
                .size(163.15.dp, 163.15.dp)
                .background(Color(0xFF3E7E0C), SoftBoomShape),
        )
        Text(
            text = "t.me/FemClaudeMrrr",
            modifier = Modifier
                .offset(0.dp, 466.11.dp)
                .alpha(0.4f)
                .clickable(onClick = { uriHandler.openUri("https://t.me/FemClaudeMrrr") }),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 36.71.sp,
            fontWeight = FontWeight.W700,
            softWrap = false,
        )
        Box(
            Modifier
                .offset(371.2.dp, 351.1.dp)
                .size(281.5.dp, 281.5.dp)
                .background(Color(0xFF10802B), VerySunnyShape),
        )
        Text(
            text = "Miko",
            modifier = Modifier
                .offset(169.2.dp, 208.4.dp),
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

private val Cookie9Shape: Shape = cookie9().toComposeShape()
private val SoftBurstShape: Shape = softBurst().toComposeShape()
private val PentagonShape: Shape = pentagon().toComposeShape()
private val Star5Shape: Shape = RoundedPolygon.star(5).normalized().toComposeShape()
private val SoftBoomShape: Shape = softBoom().toComposeShape()
private val VerySunnyShape: Shape = verySunny().toComposeShape()

private val cornerRound50 = CornerRounding(radius = 0.5f)

private data class PointNRound(
    val o: Offset,
    val r: CornerRounding = CornerRounding.Unrounded,
)

private fun cookie9(): RoundedPolygon =
    RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = .8f,
        rounding = cornerRound50,
    ).normalized()

private fun pentagon(): RoundedPolygon =
    customPolygon(
        listOf(
            PointNRound(Offset(0.500f, -0.009f), CornerRounding(0.172f)),
            PointNRound(Offset(1.030f, 0.365f), CornerRounding(0.164f)),
            PointNRound(Offset(0.828f, 0.970f), CornerRounding(0.169f)),
        ),
        reps = 1,
        mirroring = true,
    ).normalized()

private fun verySunny(): RoundedPolygon =
    customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 1.080f), CornerRounding(0.085f)),
            PointNRound(Offset(0.358f, 0.843f), CornerRounding(0.085f)),
        ),
        8,
    ).normalized()

private fun softBurst(): RoundedPolygon =
    customPolygon(
        listOf(
            PointNRound(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
            PointNRound(Offset(0.176f, 0.055f), CornerRounding(0.053f)),
        ),
        reps = 10,
    ).normalized()

private fun softBoom(): RoundedPolygon =
    customPolygon(
        listOf(
            PointNRound(Offset(0.733f, 0.454f)),
            PointNRound(Offset(0.839f, 0.437f), CornerRounding(0.532f)),
            PointNRound(Offset(0.949f, 0.449f), CornerRounding(0.439f, 1.000f)),
            PointNRound(Offset(0.998f, 0.478f), CornerRounding(0.174f)),
        ),
        reps = 16,
        mirroring = true,
    ).normalized()

private fun customPolygon(
    points: List<PointNRound>,
    reps: Int,
    center: Offset = Offset(0.5f, 0.5f),
    mirroring: Boolean = false,
): RoundedPolygon {
    val actual = doRepeat(points, reps, center, mirroring)
    return RoundedPolygon(
        vertices = FloatArray(actual.size * 2) { ix ->
            actual[ix / 2].o.let { if (ix % 2 == 0) it.x else it.y }
        },
        perVertexRounding = actual.map { it.r },
        centerX = center.x,
        centerY = center.y,
    )
}

private fun doRepeat(
    points: List<PointNRound>,
    reps: Int,
    center: Offset,
    mirroring: Boolean,
): List<PointNRound> =
    if (mirroring) {
        buildList {
            val angles = points.map { (it.o - center).angleDegrees() }
            val distances = points.map { (it.o - center).getDistance() }
            val actualReps = reps * 2
            val sectionAngle = 360f / actualReps
            repeat(actualReps) {
                points.indices.forEach { index ->
                    val i = if (it % 2 == 0) index else points.lastIndex - index
                    if (i > 0 || it % 2 == 0) {
                        val angle = (sectionAngle * it +
                            if (it % 2 == 0) angles[i]
                            else sectionAngle - angles[i] + 2 * angles[0]).toRadians()
                        val point = Offset(cos(angle), sin(angle)) * distances[i] + center
                        add(PointNRound(point, points[i].r))
                    }
                }
            }
        }
    } else {
        points.size.let { count ->
            (0 until count * reps).map {
                val point = points[it % count].o.rotateDegrees((it / count) * 360f / reps, center)
                PointNRound(point, points[it % count].r)
            }
        }
    }

private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero): Offset =
    angle.toRadians().let { radians ->
        val off = this - center
        Offset(
            off.x * cos(radians) - off.y * sin(radians),
            off.x * sin(radians) + off.y * cos(radians),
        ) + center
    }

private fun Float.toRadians(): Float = this / 360f * 2 * PI.toFloat()

private fun Offset.angleDegrees(): Float = atan2(y, x) * 180f / PI.toFloat()

private fun RoundedPolygon.toComposeShape(): Shape = GenericShape { size, _ ->
    val path = toPath().asComposePath()
    path.transform(Matrix().apply { scale(size.width, size.height) })
    addPath(path)
}
