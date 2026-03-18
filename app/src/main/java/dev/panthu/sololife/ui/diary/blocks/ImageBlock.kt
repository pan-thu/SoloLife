package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.panthu.sololife.util.pathToUri

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBlock(
    paths: List<String>,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove image block?") },
            text = { Text("This will remove all ${paths.size} image(s) in this block.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDeleteBlock() }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val shape = RoundedCornerShape(8.dp)
    if (paths.size == 1) {
        AsyncImage(
            model = pathToUri(context, paths[0]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(shape)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showDeleteDialog = true }
                )
        )
    } else {
        val chunked = paths.chunked(2)
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            chunked.forEach { rowPaths ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowPaths.forEach { path ->
                        AsyncImage(
                            model = pathToUri(context, path),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(shape)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { showDeleteDialog = true }
                                )
                        )
                    }
                    // Pad last row if odd number of images
                    if (rowPaths.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
