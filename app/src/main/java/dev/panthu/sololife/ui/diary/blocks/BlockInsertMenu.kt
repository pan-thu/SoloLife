package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class InsertBlockType { TEXT, IMAGE, DIVIDER, CHECKLIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockInsertMenu(
    onInsert: (InsertBlockType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "INSERT BLOCK",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BlockTile(emoji = "📝", label = "Text", subtitle = "Paragraph",
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.TEXT) }
            BlockTile(emoji = "🖼️", label = "Image", subtitle = "From gallery",
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.IMAGE) }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BlockTile(emoji = "➖", label = "Divider", subtitle = "Horizontal line",
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.DIVIDER) }
            BlockTile(emoji = "☑️", label = "Checklist", subtitle = "Todo items",
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.CHECKLIST) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BlockTile(
    emoji: String,
    label: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
        Column {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
