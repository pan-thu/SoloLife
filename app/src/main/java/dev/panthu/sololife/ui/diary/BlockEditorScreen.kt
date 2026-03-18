package dev.panthu.sololife.ui.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import dev.panthu.sololife.data.model.Block
import dev.panthu.sololife.data.model.CheckItem
import dev.panthu.sololife.ui.diary.blocks.*
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.deleteImage
import dev.panthu.sololife.util.saveImage
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorScreen(
    entryId: Long?,
    onBack: () -> Unit,
    vm: DiaryViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var blocks by remember { mutableStateOf<List<Block>>(emptyList()) }
    val richTextStates = remember { mutableStateMapOf<String, RichTextState>() }
    var selectedBlockId by remember { mutableStateOf<String?>(null) }
    var showInsertMenu by remember { mutableStateOf(false) }
    var pendingDeletePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var loaded by remember { mutableStateOf(entryId == null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Load existing entry on first composition.
    // RichTextState is constructed imperatively here (not via rememberRichTextState in a loop —
    // calling remember-backed functions inside a forEach violates Compose slot-table ordering).
    LaunchedEffect(entryId) {
        if (entryId != null) {
            val result = vm.getEntryAsBlocks(entryId)
            if (result == null) { onBack(); return@LaunchedEffect }
            val (loadedTitle, loadedBlocks) = result
            title = loadedTitle
            loadedBlocks.forEach { block ->
                if (block is Block.Text) {
                    val state = RichTextState()
                    if (block.html.trimStart().startsWith("<")) {
                        state.setHtml(block.html)
                    } else if (block.html.isNotBlank()) {
                        state.setText(block.html)
                    }
                    richTextStates[block.id] = state
                }
            }
            blocks = loadedBlocks
            loaded = true
        } else {
            // New entry: start with one empty Text block
            val firstId = UUID.randomUUID().toString()
            richTextStates[firstId] = RichTextState()
            blocks = listOf(Block.Text(id = firstId, html = ""))
            loaded = true
        }
    }

    // Image picker — launched from BlockInsertMenu
    var pendingImageInsertAfterBlockId by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val paths = uris.map { saveImage(context, it) }
                val newBlock = Block.Image(id = UUID.randomUUID().toString(), paths = paths)
                val insertAfter = pendingImageInsertAfterBlockId
                blocks = blocks.toMutableList().also { list ->
                    val idx = list.indexOfFirst { it.id == insertAfter }
                    list.add(if (idx >= 0) idx + 1 else list.size, newBlock)
                }
                pendingImageInsertAfterBlockId = null
            }
        } else {
            pendingImageInsertAfterBlockId = null
        }
    }

    fun isEmptyEntry(): Boolean {
        if (title.isNotBlank()) return false
        if (blocks.isEmpty()) return true
        if (blocks.size == 1 && blocks[0] is Block.Text) {
            val state = richTextStates[blocks[0].id]
            return state == null || state.annotatedString.text.isBlank()
        }
        return false
    }

    fun save() {
        if (isSaving) return
        if (isEmptyEntry()) {
            blocks.filterIsInstance<Block.Image>().flatMap { it.paths }.forEach { deleteImage(it) }
            pendingDeletePaths.forEach { deleteImage(it) }
            onBack()
            return
        }
        isSaving = true
        // Flush RichTextState HTML back into block list before saving
        val updatedBlocks = blocks.map { block ->
            if (block is Block.Text) {
                block.copy(html = richTextStates[block.id]?.toHtml() ?: block.html)
            } else block
        }
        scope.launch {
            try {
                vm.saveBlocks(entryId, title.trim(), updatedBlocks, date)
                pendingDeletePaths.forEach { deleteImage(it) }
                onBack()
            } catch (e: Exception) {
                isSaving = false
            }
        }
    }

    fun insertBlock(type: InsertBlockType) {
        showInsertMenu = false
        val afterBlockId = selectedBlockId?.takeIf { id -> blocks.any { it.id == id } }
        when (type) {
            InsertBlockType.TEXT -> {
                val newId = UUID.randomUUID().toString()
                richTextStates[newId] = RichTextState()
                val newBlock = Block.Text(id = newId, html = "")
                blocks = blocks.toMutableList().also { list ->
                    val idx = if (afterBlockId != null) list.indexOfFirst { it.id == afterBlockId } else -1
                    list.add(if (idx >= 0) idx + 1 else list.size, newBlock)
                }
                selectedBlockId = newId
            }
            InsertBlockType.IMAGE -> {
                if (pendingImageInsertAfterBlockId == null) {
                    pendingImageInsertAfterBlockId = afterBlockId
                    imagePicker.launch("image/*")
                }
            }
            InsertBlockType.DIVIDER -> {
                val newBlock = Block.Divider(id = UUID.randomUUID().toString())
                blocks = blocks.toMutableList().also { list ->
                    val idx = if (afterBlockId != null) list.indexOfFirst { it.id == afterBlockId } else -1
                    list.add(if (idx >= 0) idx + 1 else list.size, newBlock)
                }
            }
            InsertBlockType.CHECKLIST -> {
                val newBlock = Block.Checklist(
                    id = UUID.randomUUID().toString(),
                    items = listOf(CheckItem(id = UUID.randomUUID().toString(), text = "", checked = false))
                )
                blocks = blocks.toMutableList().also { list ->
                    val idx = if (afterBlockId != null) list.indexOfFirst { it.id == afterBlockId } else -1
                    list.add(if (idx >= 0) idx + 1 else list.size, newBlock)
                }
            }
        }
    }

    fun moveBlockUp(blockId: String) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx > 0) {
            blocks = blocks.toMutableList().also { list ->
                val tmp = list[idx]; list[idx] = list[idx - 1]; list[idx - 1] = tmp
            }
        }
    }

    fun moveBlockDown(blockId: String) {
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx >= 0 && idx < blocks.lastIndex) {
            blocks = blocks.toMutableList().also { list ->
                val tmp = list[idx]; list[idx] = list[idx + 1]; list[idx + 1] = tmp
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = ::save) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Save & go back")
                    }
                },
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "Change date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = DateUtils.formatShort(date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::save) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showInsertMenu = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add block")
            }
        }
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { selectedBlockId = null }
        ) {
            NotePageBackground(modifier = Modifier.fillMaxSize())

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 28.dp,
                    bottom = 80.dp
                )
            ) {
                // Title
                item {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = TextStyle(
                            fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = MaterialTheme.typography.headlineLarge.lineHeight,
                            fontFamily = CaveatFontFamily
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        "Title",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = CaveatFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
                item {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
                // Block rows
                itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                    BlockRow(
                        block = block,
                        richTextStates = richTextStates,
                        isSelected = selectedBlockId == block.id,
                        onSelect = { selectedBlockId = block.id },
                        onFocusChange = { id, focused -> if (focused) selectedBlockId = id },
                        onDeleteImageBlock = { paths ->
                            pendingDeletePaths = pendingDeletePaths + paths
                            blocks = blocks.filter { it.id != block.id }
                        },
                        onDeleteBlock = {
                            when (block) {
                                is Block.Text -> richTextStates.remove(block.id)
                                is Block.Image -> pendingDeletePaths = pendingDeletePaths + block.paths
                                else -> Unit
                            }
                            blocks = blocks.filter { it.id != block.id }
                        },
                        onChecklistChanged = { newItems ->
                            blocks = blocks.map {
                                if (it.id == block.id) (it as Block.Checklist).copy(items = newItems) else it
                            }
                        },
                        isFirst = index == 0,
                        isLast = index == blocks.lastIndex,
                        onMoveUp = { moveBlockUp(block.id) },
                        onMoveDown = { moveBlockDown(block.id) }
                    )
                }
            }

            // Formatting bubble — shown when a text block is selected
            val selectedBlock = blocks.find { it.id == selectedBlockId }
            val focusedState = if (selectedBlock is Block.Text) richTextStates[selectedBlockId] else null
            if (focusedState != null) {
                FormattingBubble(
                    state = focusedState,
                    onDismiss = { selectedBlockId = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }

        // Insert menu sheet
        if (showInsertMenu) {
            BlockInsertMenu(
                onInsert = { type -> insertBlock(type) },
                onDismiss = { showInsertMenu = false }
            )
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { utcMillis ->
                        val localDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        date = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun BlockRow(
    block: Block,
    richTextStates: Map<String, RichTextState>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFocusChange: (String, Boolean) -> Unit,
    onDeleteImageBlock: (List<String>) -> Unit,
    onDeleteBlock: () -> Unit,
    onChecklistChanged: (List<CheckItem>) -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Block content
        when (block) {
            is Block.Text -> {
                val state = richTextStates[block.id]
                if (state != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        TextBlock(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                            onFocusChanged = { focused -> onFocusChange(block.id, focused) }
                        )
                    }
                }
            }
            is Block.Image -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect() }
                ) {
                    ImageBlock(
                        paths = block.paths,
                        onDeleteBlock = { onDeleteImageBlock(block.paths) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
            is Block.Divider -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect() }
                ) {
                    DividerBlock(modifier = Modifier.fillMaxWidth())
                }
            }
            is Block.Checklist -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect() }
                ) {
                    ChecklistBlock(
                        items = block.items,
                        onItemsChanged = onChecklistChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
        // Reorder + delete column — only shown when this block is selected
        if (isSelected) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp)
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
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Move block down",
                        modifier = Modifier.size(18.dp),
                        tint = if (!isLast) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onDeleteBlock,
                    modifier = Modifier.size(32.dp)
                ) {
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
}
