package dev.panthu.sololife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Canvas-drawn open-book illustration for the diary empty state.
 *
 * Illustration (200×120dp):
 *   - Two rounded rectangles side by side (left page, right page)
 *   - Thin spine line down the centre
 *   - 3–4 horizontal text lines on each page (varying 40–80% page width)
 *   - Quill/pen: diagonal line with a small V feather tip at the top
 *
 * Below: title "No entries yet" + subtitle "Start writing your first diary entry"
 *
 * Colors:
 *   outline at 30% alpha for book shapes/lines
 *   primary at 60% alpha for the quill
 */
@Composable
fun DiaryEmptyState(modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(width = 200.dp, height = 120.dp)) {
            val bookColor = outlineColor.copy(alpha = 0.30f)
            val quillColor = primaryColor.copy(alpha = 0.60f)

            val canvasW = size.width
            val canvasH = size.height

            // Book occupies the centre of the canvas, leaving room for the quill.
            // Each page: ~38% of canvas width, height ~70% of canvas height
            val pageW = canvasW * 0.36f
            val pageH = canvasH * 0.72f
            val bookTop = (canvasH - pageH) / 2f
            val spineX = canvasW / 2f

            val cornerR = CornerRadius(12f, 12f)
            val strokeW = 2.dp.toPx()

            // Left page
            val leftPageLeft = spineX - pageW - 2f
            drawRoundRect(
                color = bookColor,
                topLeft = Offset(leftPageLeft, bookTop),
                size = Size(pageW, pageH),
                cornerRadius = cornerR,
                style = Stroke(width = strokeW)
            )

            // Right page
            val rightPageLeft = spineX + 2f
            drawRoundRect(
                color = bookColor,
                topLeft = Offset(rightPageLeft, bookTop),
                size = Size(pageW, pageH),
                cornerRadius = cornerR,
                style = Stroke(width = strokeW)
            )

            // Spine line
            drawLine(
                color = bookColor,
                start = Offset(spineX, bookTop),
                end = Offset(spineX, bookTop + pageH),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Text lines — left page (4 lines: 80%, 60%, 70%, 40% of page width)
            val lineH = 2.dp.toPx()
            val lineGap = (pageH - 8.dp.toPx()) / 5f
            val leftLineWidths = listOf(0.80f, 0.60f, 0.70f, 0.40f)
            leftLineWidths.forEachIndexed { i, fraction ->
                val y = bookTop + lineGap * (i + 1)
                val lineW = pageW * fraction
                val lx = leftPageLeft + (pageW - lineW) / 2f
                drawLine(
                    color = bookColor,
                    start = Offset(lx, y),
                    end = Offset(lx + lineW, y),
                    strokeWidth = lineH,
                    cap = StrokeCap.Round
                )
            }

            // Text lines — right page (3 lines: 75%, 55%, 65% of page width)
            val rightLineWidths = listOf(0.75f, 0.55f, 0.65f)
            rightLineWidths.forEachIndexed { i, fraction ->
                val y = bookTop + lineGap * (i + 1)
                val lineW = pageW * fraction
                val rx = rightPageLeft + (pageW - lineW) / 2f
                drawLine(
                    color = bookColor,
                    start = Offset(rx, y),
                    end = Offset(rx + lineW, y),
                    strokeWidth = lineH,
                    cap = StrokeCap.Round
                )
            }

            // Quill — positioned to the right of the book, angled diagonally
            val quillStroke = 2.dp.toPx()
            val quillStartX = rightPageLeft + pageW + 12.dp.toPx()
            val quillStartY = bookTop + 10.dp.toPx()
            val quillEndX = quillStartX - 20.dp.toPx()
            val quillEndY = quillStartY + 48.dp.toPx()

            // Shaft of the quill
            drawLine(
                color = quillColor,
                start = Offset(quillStartX, quillStartY),
                end = Offset(quillEndX, quillEndY),
                strokeWidth = quillStroke,
                cap = StrokeCap.Round
            )

            // Feather tip — small V at the top of the shaft
            val tipLength = 10.dp.toPx()
            // Left barb
            drawLine(
                color = quillColor,
                start = Offset(quillStartX, quillStartY),
                end = Offset(quillStartX - tipLength, quillStartY - tipLength * 0.6f),
                strokeWidth = quillStroke,
                cap = StrokeCap.Round
            )
            // Right barb
            drawLine(
                color = quillColor,
                start = Offset(quillStartX, quillStartY),
                end = Offset(quillStartX + tipLength * 0.5f, quillStartY - tipLength),
                strokeWidth = quillStroke,
                cap = StrokeCap.Round
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "No entries yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Start writing your first diary entry",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Canvas-drawn wallet illustration for the expenses empty state.
 *
 * Illustration (160×120dp):
 *   - Wallet body: rounded rectangle (main body)
 *   - Card slot: smaller rounded rectangle inside the top of the wallet
 *   - Coin: circle with a "$" text drawn at centre
 *
 * Below: title "No expenses yet" + subtitle "Track your first expense"
 *
 * Colors:
 *   outline at 30% alpha for wallet shapes
 *   tertiary at 60% alpha for the coin
 */
@Composable
fun ExpensesEmptyState(modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(width = 160.dp, height = 120.dp)) {
            val walletColor = outlineColor.copy(alpha = 0.30f)
            val coinColor = tertiaryColor.copy(alpha = 0.60f)

            val strokeW = 2.dp.toPx()
            val canvasW = size.width
            val canvasH = size.height

            // Wallet body — centred, occupies ~60% width × 55% height
            val walletW = canvasW * 0.60f
            val walletH = canvasH * 0.55f
            val walletLeft = (canvasW - walletW) / 2f
            val walletTop = canvasH * 0.15f
            val walletCorner = CornerRadius(14f, 14f)

            drawRoundRect(
                color = walletColor,
                topLeft = Offset(walletLeft, walletTop),
                size = Size(walletW, walletH),
                cornerRadius = walletCorner,
                style = Stroke(width = strokeW)
            )

            // Card slot — inside the top quarter of the wallet body
            val cardSlotW = walletW * 0.70f
            val cardSlotH = walletH * 0.28f
            val cardSlotLeft = walletLeft + (walletW - cardSlotW) / 2f
            val cardSlotTop = walletTop + walletH * 0.12f

            drawRoundRect(
                color = walletColor,
                topLeft = Offset(cardSlotLeft, cardSlotTop),
                size = Size(cardSlotW, cardSlotH),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = strokeW)
            )

            // Coin — circle to the right of the wallet, vertically centred
            val coinRadius = 18.dp.toPx()
            val coinCx = walletLeft + walletW + coinRadius + 8.dp.toPx()
            val coinCy = walletTop + walletH / 2f

            drawCircle(
                color = coinColor,
                radius = coinRadius,
                center = Offset(coinCx, coinCy),
                style = Stroke(width = strokeW)
            )

            // "$" symbol inside the coin — approximated with two short lines
            // (a simple cross-stroke: vertical bar + two small serifs for S-like shape)
            val dollarStroke = 2.dp.toPx()
            val barH = coinRadius * 0.80f
            // Vertical bar
            drawLine(
                color = coinColor,
                start = Offset(coinCx, coinCy - barH / 2f),
                end = Offset(coinCx, coinCy + barH / 2f),
                strokeWidth = dollarStroke,
                cap = StrokeCap.Round
            )
            // Top horizontal serif
            drawLine(
                color = coinColor,
                start = Offset(coinCx - coinRadius * 0.35f, coinCy - barH * 0.25f),
                end = Offset(coinCx + coinRadius * 0.35f, coinCy - barH * 0.25f),
                strokeWidth = dollarStroke,
                cap = StrokeCap.Round
            )
            // Bottom horizontal serif
            drawLine(
                color = coinColor,
                start = Offset(coinCx - coinRadius * 0.35f, coinCy + barH * 0.25f),
                end = Offset(coinCx + coinRadius * 0.35f, coinCy + barH * 0.25f),
                strokeWidth = dollarStroke,
                cap = StrokeCap.Round
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "No expenses yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Track your first expense",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
