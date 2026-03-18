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
    val marginLinePx = with(density) { 40.dp.toPx() }
    val lineColor = Color(0xFFB8D4E8)
    val marginColor = Color(0xFFFFB3B3)

    // Pre-compute stroke widths outside DrawScope (toPx() needs Density receiver)
    val ruleStrokePx = with(density) { 1.dp.toPx() }
    val marginStrokePx = with(density) { 1.5.dp.toPx() }

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
        // Vertical red margin line at x = 40dp
        drawLine(
            color = marginColor,
            start = Offset(marginLinePx, 0f),
            end = Offset(marginLinePx, size.height),
            alpha = 0.4f,
            strokeWidth = marginStrokePx
        )
    }
}
