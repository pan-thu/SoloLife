package dev.panthu.sololife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer

/**
 * Shimmer skeleton that mirrors the shape of a DiaryTimelineItem.
 *
 * Layout:
 *   Row
 *     Left column (40dp): timeline dot circle + vertical connector line
 *     Right card (Surface, 20dp corners):
 *       title line 70% width × 14dp
 *       spacer 8dp
 *       date line  40% width × 10dp
 *       spacer 12dp
 *       preview line 1  90% width × 12dp
 *       preview line 2  60% width × 12dp
 */
@Composable
fun ShimmerDiaryCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shimmer(),
        verticalAlignment = Alignment.Top
    ) {
        // ── Left timeline column ──────────────────────────────────────────────
        Column(
            modifier = Modifier.width(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timeline dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(4.dp))
            // Connector line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        Spacer(Modifier.width(8.dp))

        // ── Right card ────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Title line — 70% width
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(Modifier.height(8.dp))

                // Date line — 40% width
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(Modifier.height(12.dp))

                // Preview line 1 — 90% width
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(Modifier.height(6.dp))

                // Preview line 2 — 60% width
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

/**
 * Shimmer skeleton that mirrors the shape of an ExpenseRow.
 *
 * Layout:
 *   Row (horizontal padding)
 *     Left:   category dot circle 10dp
 *     Middle (weight 1f):
 *       category name  30% × 12dp
 *       description    50% × 10dp
 *     Right:  amount   25% × 14dp
 */
@Composable
fun ShimmerExpenseCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .shimmer()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Category dot ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(Modifier.width(14.dp))

        // ── Middle: name + description ────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            // Category name — 30% width
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.30f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(Modifier.height(6.dp))

            // Description — 50% width
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.50f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        Spacer(Modifier.width(8.dp))

        // ── Amount — 25% width ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
