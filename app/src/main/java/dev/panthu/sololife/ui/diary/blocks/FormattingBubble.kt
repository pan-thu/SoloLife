package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Bottom action bar shown when any block is selected.
 *
 * [state] — non-null for text blocks: shows B/I/U/bullet formatting buttons.
 * [isFirst]/[isLast] — controls enabled state of the move-up/move-down buttons.
 */
@Composable
fun FormattingBubble(
    state: RichTextState?,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Formatting buttons — only for text blocks
            if (state != null) {
                val isBold = state.currentSpanStyle.fontWeight == FontWeight.Bold
                val isItalic = state.currentSpanStyle.fontStyle == FontStyle.Italic
                val isUnderline = state.currentSpanStyle.textDecoration
                    ?.contains(TextDecoration.Underline) == true
                val isBullet = state.isUnorderedList

                BubbleButton(Icons.Rounded.FormatBold, isBold, "Bold") {
                    state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                }
                BubbleButton(Icons.Rounded.FormatItalic, isItalic, "Italic") {
                    state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                }
                BubbleButton(Icons.Rounded.FormatUnderlined, isUnderline, "Underline") {
                    state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                }
                BubbleButton(Icons.AutoMirrored.Rounded.FormatListBulleted, isBullet, "Bullet list") {
                    state.toggleUnorderedList()
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Move up / down / delete — always shown for selected block
            IconButton(
                onClick = onMoveUp,
                enabled = !isFirst,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move block up",
                    modifier = Modifier.size(18.dp),
                    tint = if (!isFirst) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = !isLast,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move block down",
                    modifier = Modifier.size(18.dp),
                    tint = if (!isLast) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete block",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun BubbleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
