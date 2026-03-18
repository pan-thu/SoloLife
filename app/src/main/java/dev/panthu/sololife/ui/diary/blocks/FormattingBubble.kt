package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

@Composable
fun FormattingBubble(
    state: RichTextState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBold = state.currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = state.currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline = state.currentSpanStyle.textDecoration
        ?.contains(TextDecoration.Underline) == true
    val isBullet = state.isUnorderedList

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            BubbleButton(icon = Icons.Rounded.FormatBold, active = isBold, contentDescription = "Bold") {
                state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            }
            BubbleButton(icon = Icons.Rounded.FormatItalic, active = isItalic, contentDescription = "Italic") {
                state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            }
            BubbleButton(icon = Icons.Rounded.FormatUnderlined, active = isUnderline, contentDescription = "Underline") {
                state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            BubbleButton(icon = Icons.Rounded.FormatListBulleted, active = isBullet, contentDescription = "Bullet list") {
                state.toggleUnorderedList()
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
            tint = if (active)
                MaterialTheme.colorScheme.inverseOnSurface
            else
                MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}
