package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            BlockTile(icon = Icons.Rounded.Notes, label = "Text", subtitle = "Paragraph",
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.TEXT) }
            BlockTile(icon = Icons.Rounded.Image, label = "Image", subtitle = "From gallery",
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.IMAGE) }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BlockTile(icon = Icons.Rounded.HorizontalRule, label = "Divider", subtitle = "Horizontal line",
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.DIVIDER) }
            BlockTile(icon = Icons.Rounded.Checklist, label = "Checklist", subtitle = "Todo items",
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.CHECKLIST) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BlockTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    color: Color,
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
