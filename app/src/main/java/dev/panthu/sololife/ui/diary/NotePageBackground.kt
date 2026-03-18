package dev.panthu.sololife.ui.diary

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun NotePageBackground(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val lineSpacingPx = with(density) { 28.dp.toPx() }
    val lineColor = Color(0xFFB8D4E8)

    // Pre-compute stroke width outside DrawScope (toPx() needs Density receiver)
    val ruleStrokePx = with(density) { 1.dp.toPx() }

    Canvas(modifier = modifier) {
        // Horizontal ruled lines every 28dp starting at y = 28dp
        var y = lineSpacingPx
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                alpha = 0.35f,
                strokeWidth = ruleStrokePx
            )
            y += lineSpacingPx
        }
    }
}
