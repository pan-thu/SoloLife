package dev.panthu.sololife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*

@Composable
fun AmbientOrbs(
    primary: Color,
    tertiary: Color,
    modifier: Modifier = Modifier.Companion
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")

    val drift1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift1"
    )
    val drift2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(12000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift2"
    )

    Canvas(modifier = modifier) {
        val orb1X = size.width * 0.10f + drift1 * size.width * 0.08f
        val orb1Y = size.height * 0.05f + drift1 * size.height * 0.04f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = 0.38f), Color.Transparent),
                center = Offset(orb1X, orb1Y),
                radius = size.width * 0.55f
            ),
            center = Offset(orb1X, orb1Y),
            radius = size.width * 0.55f
        )

        val orb2X = size.width * 0.88f - drift2 * size.width * 0.08f
        val orb2Y = size.height * 0.28f + drift2 * size.height * 0.05f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(tertiary.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(orb2X, orb2Y),
                radius = size.width * 0.48f
            ),
            center = Offset(orb2X, orb2Y),
            radius = size.width * 0.48f
        )
    }
}
