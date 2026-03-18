package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.panthu.sololife.data.model.CheckItem
import java.util.UUID

@Composable
fun ChecklistBlock(
    items: List<CheckItem>,
    onItemsChanged: (List<CheckItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) {
            onItemsChanged(listOf(CheckItem(id = UUID.randomUUID().toString(), text = "", checked = false)))
        }
    }
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { checked ->
                        onItemsChanged(items.toMutableList().also { it[index] = item.copy(checked = checked) })
                    }
                )
                BasicTextField(
                    value = item.text,
                    onValueChange = { newText ->
                        onItemsChanged(items.toMutableList().also { it[index] = item.copy(text = newText) })
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (item.checked)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            val newItem = CheckItem(id = UUID.randomUUID().toString(), text = "", checked = false)
                            val newList = items.toMutableList().apply { add(index + 1, newItem) }
                            onItemsChanged(newList)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
            }
        }
    }
}
