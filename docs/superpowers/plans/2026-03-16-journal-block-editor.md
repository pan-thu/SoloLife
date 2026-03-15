# Journal Block Editor — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single-`RichTextEditor` diary entry screen with a block-based notebook editor featuring a ruled-paper aesthetic, inline images, checklists, and dividers — all stored in Room as JSON.

**Architecture:** Each diary entry stores its content as a JSON array of typed blocks (`Text`, `Image`, `Divider`, `Checklist`) in a new `blocksJson` Room column. The new `BlockEditorScreen` renders blocks in a `LazyColumn` over a Canvas-drawn ruled-paper background; a `+` button in each block's left margin opens a `ModalBottomSheet` to insert new blocks. Text formatting is handled by a floating `Popup` bubble triggered on text selection. Legacy entries (plain HTML in `content`) are migrated transparently on first open.

**Tech Stack:** Kotlin 2.0.21 · Jetpack Compose (BOM 2024.09.00) · Material3 · Room 2.7.0 · `kotlinx-serialization-json` 1.7.3 · `richeditor-compose` 1.0.0-rc10 · Coil 2.7.0

---

## Chunk 1: Data layer — Block model, DB migration, ViewModel

### Task 1: Add `Block` sealed class

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/data/model/Block.kt`

- [ ] **Create `Block.kt`**

```kotlin
package dev.panthu.sololife.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Block {
    @Serializable @SerialName("text")
    data class Text(val id: String, val html: String) : Block()

    @Serializable @SerialName("image")
    data class Image(val id: String, val paths: List<String>) : Block()

    @Serializable @SerialName("divider")
    data class Divider(val id: String) : Block()

    @Serializable @SerialName("checklist")
    data class Checklist(val id: String, val items: List<CheckItem>) : Block()
}

@Serializable
data class CheckItem(val id: String, val text: String, val checked: Boolean)
```

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/data/model/Block.kt
git commit -m "feat(diary): add Block sealed class and CheckItem model"
```

---

### Task 2: Add `blocksJson` column to `DiaryEntry` and bump DB to v3

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/data/db/DiaryEntry.kt`
- Modify: `app/src/main/java/dev/panthu/sololife/data/db/SoloLifeDatabase.kt`

- [ ] **Add `blocksJson` to `DiaryEntry`**

In `DiaryEntry.kt`, add one field at the end of the data class:

```kotlin
val blocksJson: String = ""   // JSON array of Block; "" = legacy entry
```

Full file after change:

```kotlin
package dev.panthu.sololife.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["date"])]
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUris: String = "",
    val blocksJson: String = ""
)
```

- [ ] **Bump database version and add migration in `SoloLifeDatabase.kt`**

Change `version = 2` → `version = 3` and add `MIGRATION_2_3`:

```kotlin
@Database(
    entities = [DiaryEntry::class, Expense::class],
    version = 3,
    exportSchema = false
)
abstract class SoloLifeDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: SoloLifeDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN imageUris TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diary_entries ADD COLUMN blocksJson TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): SoloLifeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoloLifeDatabase::class.java,
                    "sololife.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
```

- [ ] **Build to confirm no compile errors**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/data/db/DiaryEntry.kt \
        app/src/main/java/dev/panthu/sololife/data/db/SoloLifeDatabase.kt
git commit -m "feat(diary): add blocksJson column, MIGRATION_2_3, bump DB to v3"
```

---

### Task 3: Add `saveBlocks` and `getEntryAsBlocks` to `DiaryViewModel`

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryViewModel.kt`

These two functions are the core data bridge between the editor UI and Room.

- [ ] **Add imports and the two new functions to `DiaryViewModel`**

Add at the top of the file (alongside existing imports):

```kotlin
import dev.panthu.sololife.data.model.Block
import dev.panthu.sololife.data.model.CheckItem
import dev.panthu.sololife.util.encodeUris
import dev.panthu.sololife.util.parseUris
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
```

Add these two functions inside `DiaryViewModel`, after the existing `save()` function:

```kotlin
/** Load an entry and return (title, blocks). Migrates legacy entries transparently. */
suspend fun getEntryAsBlocks(id: Long): Pair<String, List<Block>>? {
    val entry = repo.getById(id) ?: return null
    if (entry.blocksJson.isNotBlank()) {
        val blocks = Json.decodeFromString<List<Block>>(entry.blocksJson)
        return entry.title to blocks
    }
    // Legacy migration: convert HTML content + imageUris → blocks
    val textBlock = Block.Text(
        id = java.util.UUID.randomUUID().toString(),
        html = entry.content
    )
    val imageBlock = if (entry.imageUris.isNotBlank()) {
        Block.Image(
            id = java.util.UUID.randomUUID().toString(),
            paths = parseUris(entry.imageUris)
        )
    } else null
    val blocks = listOfNotNull(textBlock, imageBlock)
    // Persist immediately so migration only runs once
    saveBlocks(id, entry.title, blocks, entry.date)
    return entry.title to blocks
}

/** Serialize blocks, keep content + imageUris in sync for search, persist. */
suspend fun saveBlocks(
    id: Long?,
    title: String,
    blocks: List<Block>,
    date: Long
) {
    val blocksJson = Json.encodeToString(blocks)
    // Derive content: concat all Text block HTML for search DAO compatibility
    val content = blocks.filterIsInstance<Block.Text>()
        .joinToString("\n") { it.html }
    // Derive imageUris: collect all Image block paths for backward compat
    val imageUris = encodeUris(
        blocks.filterIsInstance<Block.Image>().flatMap { it.paths }
    )
    if (id == null) {
        repo.save(
            DiaryEntry(
                date = date,
                title = title,
                content = content,
                imageUris = imageUris,
                blocksJson = blocksJson
            )
        )
    } else {
        val existing = repo.getById(id) ?: return
        repo.update(
            existing.copy(
                title = title,
                content = content,
                imageUris = imageUris,
                blocksJson = blocksJson,
                date = date,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
```

- [ ] **Build to confirm no compile errors**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/DiaryViewModel.kt
git commit -m "feat(diary): add saveBlocks and getEntryAsBlocks to DiaryViewModel"
```

---

## Chunk 2: Block composables

### Task 4: `NotePageBackground` — ruled paper Canvas

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/NotePageBackground.kt`

- [ ] **Create `NotePageBackground.kt`**

```kotlin
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
```

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/NotePageBackground.kt
git commit -m "feat(diary): add NotePageBackground ruled-paper Canvas composable"
```

---

### Task 5: `DividerBlock` — gradient horizontal rule

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/blocks/DividerBlock.kt`

- [ ] **Create `DividerBlock.kt`**

```kotlin
package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DividerBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFFAAAAAA), Color.Transparent)
                    ),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height
                )
            }
    )
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/blocks/DividerBlock.kt
git commit -m "feat(diary): add DividerBlock composable"
```

---

### Task 6: `TextBlock` — rich text editor block

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/blocks/TextBlock.kt`

- [ ] **Create `TextBlock.kt`**

```kotlin
package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

@Composable
fun TextBlock(
    state: RichTextState,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    RichTextEditor(
        state = state,
        modifier = modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        textStyle = LocalTextStyle.current.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        ),
        placeholder = {
            Text(
                text = "Write something…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        colors = RichTextEditorDefaults.richTextEditorColors(
            containerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/blocks/TextBlock.kt
git commit -m "feat(diary): add TextBlock composable"
```

---

### Task 7: `ImageBlock` — auto-grid image display

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/blocks/ImageBlock.kt`

- [ ] **Create `ImageBlock.kt`**

Single image = full-width card; 2+ = 2-column grid. Long-press shows delete confirmation dialog for the whole block.

```kotlin
package dev.panthu.sololife.ui.diary.blocks

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = false
        ) {
            items(paths) { path ->
                AsyncImage(
                    model = pathToUri(context, path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(shape)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showDeleteDialog = true }
                        )
                )
            }
        }
    }
}
```

- [ ] **Build to catch any Coil/richeditor API mismatches early**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/blocks/ImageBlock.kt
git commit -m "feat(diary): add ImageBlock composable with auto-grid and long-press delete"
```

---

### Task 8: `ChecklistBlock` — checkbox list with add-on-enter

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/blocks/ChecklistBlock.kt`

- [ ] **Create `ChecklistBlock.kt`**

```kotlin
package dev.panthu.sololife.ui.diary.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
                            // Append new empty item after current index
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
        // Add first item if list is empty
        if (items.isEmpty()) {
            LaunchedEffect(Unit) {
                onItemsChanged(listOf(CheckItem(id = UUID.randomUUID().toString(), text = "", checked = false)))
            }
        }
    }
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/blocks/ChecklistBlock.kt
git commit -m "feat(diary): add ChecklistBlock composable"
```

---

### Task 9: `FormattingBubble` — floating format toolbar on text selection

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/blocks/FormattingBubble.kt`

- [ ] **Create `FormattingBubble.kt`**

The bubble appears above the active `RichTextEditor` when the selection is non-collapsed.

```kotlin
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
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
                BubbleButton(
                    icon = Icons.Rounded.FormatBold,
                    active = isBold,
                    contentDescription = "Bold"
                ) { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }

                BubbleButton(
                    icon = Icons.Rounded.FormatItalic,
                    active = isItalic,
                    contentDescription = "Italic"
                ) { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }

                BubbleButton(
                    icon = Icons.Rounded.FormatUnderlined,
                    active = isUnderline,
                    contentDescription = "Underline"
                ) { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }

                BubbleButton(
                    icon = Icons.Rounded.FormatListBulleted,
                    active = isBullet,
                    contentDescription = "Bullet list"
                ) { state.toggleUnorderedList() }
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
```

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/blocks/FormattingBubble.kt
git commit -m "feat(diary): add FormattingBubble popup composable"
```

---

### Task 10: `BlockInsertMenu` — bottom sheet for inserting new blocks

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/blocks/BlockInsertMenu.kt`

- [ ] **Create `BlockInsertMenu.kt`**

```kotlin
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BlockTile(emoji = "📝", label = "Text",      subtitle = "Paragraph",
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.TEXT) }
            BlockTile(emoji = "🖼️", label = "Image",    subtitle = "From gallery",
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)) { onInsert(InsertBlockType.IMAGE) }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BlockTile(emoji = "➖", label = "Divider",   subtitle = "Horizontal line",
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
            Text(label, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

- [ ] **Build to confirm no compile errors**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/blocks/BlockInsertMenu.kt
git commit -m "feat(diary): add BlockInsertMenu bottom sheet composable"
```

---

## Chunk 3: `BlockEditorScreen` and navigation wiring

### Task 11: `BlockEditorScreen` — main editor screen

**Files:**
- Create: `app/src/main/java/dev/panthu/sololife/ui/diary/BlockEditorScreen.kt`

This is the replacement for `DiaryDetailScreen`. It wires all block composables together with the ruled-paper background, title field, `+` margin buttons, and save logic.

- [ ] **Create `BlockEditorScreen.kt`**

```kotlin
package dev.panthu.sololife.ui.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
    val richTextStates = remember { mutableStateMapOf<String, com.mohamedrejeb.richeditor.model.RichTextState>() }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    var showInsertMenuForBlockId by remember { mutableStateOf<String?>(null) }
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
                    val state = com.mohamedrejeb.richeditor.model.RichTextState()
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
            richTextStates[firstId] = com.mohamedrejeb.richeditor.model.RichTextState()
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
        if (isEmptyEntry()) { onBack(); return }
        isSaving = true
        // Flush RichTextState HTML back into block list before saving
        val updatedBlocks = blocks.map { block ->
            if (block is Block.Text) {
                block.copy(html = richTextStates[block.id]?.toHtml() ?: block.html)
            } else block
        }
        scope.launch {
            vm.saveBlocks(entryId, title.trim(), updatedBlocks, date)
            pendingDeletePaths.forEach { deleteImage(it) }
            onBack()
        }
    }

    fun insertBlock(afterBlockId: String, type: InsertBlockType) {
        showInsertMenuForBlockId = null
        when (type) {
            InsertBlockType.TEXT -> {
                val newId = UUID.randomUUID().toString()
                richTextStates[newId] = com.mohamedrejeb.richeditor.model.RichTextState()
                val newBlock = Block.Text(id = newId, html = "")
                blocks = blocks.toMutableList().also { list ->
                    val idx = list.indexOfFirst { it.id == afterBlockId }
                    list.add(if (idx >= 0) idx + 1 else list.size, newBlock)
                }
                focusedBlockId = newId
            }
            InsertBlockType.IMAGE -> {
                pendingImageInsertAfterBlockId = afterBlockId
                imagePicker.launch("image/*")
            }
            InsertBlockType.DIVIDER -> {
                val newBlock = Block.Divider(id = UUID.randomUUID().toString())
                blocks = blocks.toMutableList().also { list ->
                    val idx = list.indexOfFirst { it.id == afterBlockId }
                    list.add(if (idx >= 0) idx + 1 else list.size, newBlock)
                }
            }
            InsertBlockType.CHECKLIST -> {
                val newBlock = Block.Checklist(
                    id = UUID.randomUUID().toString(),
                    items = listOf(CheckItem(id = UUID.randomUUID().toString(), text = "", checked = false))
                )
                blocks = blocks.toMutableList().also { list ->
                    val idx = list.indexOfFirst { it.id == afterBlockId }
                    list.add(if (idx >= 0) idx + 1 else list.size, newBlock)
                }
            }
        }
    }

    Scaffold(
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
        ) {
            NotePageBackground(modifier = Modifier.fillMaxSize())

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 48.dp,
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
                            lineHeight = MaterialTheme.typography.headlineLarge.lineHeight
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(
                                        "Title",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
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
                itemsIndexed(blocks, key = { _, block -> block.id }) { _, block ->
                    BlockRow(
                        block = block,
                        richTextStates = richTextStates,
                        focusedBlockId = focusedBlockId,
                        onFocusChange = { id, focused -> if (focused) focusedBlockId = id },
                        onPlusClick = { showInsertMenuForBlockId = block.id },
                        onDeleteImageBlock = { paths ->
                            pendingDeletePaths = pendingDeletePaths + paths
                            blocks = blocks.filter { it.id != block.id }
                            // Note: if a Text block is ever deleted, also call:
                            // richTextStates.remove(block.id)
                        },
                        onChecklistChanged = { newItems ->
                            blocks = blocks.map {
                                if (it.id == block.id) (it as Block.Checklist).copy(items = newItems) else it
                            }
                        }
                    )
                }
            }

            // Formatting bubble — shown when a Text block has a non-collapsed selection
            val focusedState = focusedBlockId?.let { richTextStates[it] }
            val hasSelection = focusedState?.selection?.let { it.start != it.end } == true
            if (focusedState != null && hasSelection) {
                FormattingBubble(
                    state = focusedState,
                    onDismiss = { focusedBlockId = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 48.dp, top = 80.dp)
                )
            }
        }

        // Insert menu sheet
        if (showInsertMenuForBlockId != null) {
            BlockInsertMenu(
                onInsert = { type -> insertBlock(showInsertMenuForBlockId!!, type) },
                onDismiss = { showInsertMenuForBlockId = null }
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
    richTextStates: Map<String, com.mohamedrejeb.richeditor.model.RichTextState>,
    focusedBlockId: String?,
    onFocusChange: (String, Boolean) -> Unit,
    onPlusClick: () -> Unit,
    onDeleteImageBlock: (List<String>) -> Unit,
    onChecklistChanged: (List<CheckItem>) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // "+" button in left margin (positioned left of the 48dp content inset)
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopStart)
                .offset(x = (-38).dp, y = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onPlusClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Insert block",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
        }

        // Block content
        when (block) {
            is Block.Text -> {
                val state = richTextStates[block.id]
                if (state != null) {
                    TextBlock(
                        state = state,
                        modifier = Modifier.fillMaxWidth(),
                        onFocusChanged = { focused -> onFocusChange(block.id, focused) }
                    )
                }
            }
            is Block.Image -> {
                ImageBlock(
                    paths = block.paths,
                    onDeleteBlock = { onDeleteImageBlock(block.paths) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            is Block.Divider -> {
                DividerBlock(modifier = Modifier.fillMaxWidth())
            }
            is Block.Checklist -> {
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
}
```

- [ ] **Build to confirm no compile errors**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/BlockEditorScreen.kt
git commit -m "feat(diary): add BlockEditorScreen — block-based notebook editor"
```

---

### Task 12: Wire `BlockEditorScreen` into navigation and delete old screen

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/navigation/AppNavigation.kt`
- Delete: `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryDetailScreen.kt`

- [ ] **Update `AppNavigation.kt`** — replace `DiaryDetailScreen` with `BlockEditorScreen`

Change the two import + two composable usages. Full diff:

```kotlin
// Remove this import:
import dev.panthu.sololife.ui.diary.DiaryDetailScreen
// Add this import:
import dev.panthu.sololife.ui.diary.BlockEditorScreen
```

In the `DiaryNew` composable block, change:
```kotlin
// Before:
DiaryDetailScreen(entryId = null, onBack = { navController.popBackStack() })
// After:
BlockEditorScreen(entryId = null, onBack = { navController.popBackStack() })
```

In the `DiaryDetail` composable block, change:
```kotlin
// Before:
DiaryDetailScreen(entryId = id, onBack = { navController.popBackStack() })
// After:
BlockEditorScreen(entryId = id, onBack = { navController.popBackStack() })
```

- [ ] **Delete `DiaryDetailScreen.kt`**

```bash
git rm app/src/main/java/dev/panthu/sololife/ui/diary/DiaryDetailScreen.kt
```

- [ ] **Full build to confirm everything compiles**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/navigation/AppNavigation.kt
git commit -m "feat(diary): wire BlockEditorScreen into navigation, remove DiaryDetailScreen"
```

---

## Manual Verification Checklist

After a successful `assembleDebug`, install on a device or emulator and verify:

- [ ] **New entry:** Tap FAB → blank titled page with lined background and one empty text block
- [ ] **Type text:** Blue ruled lines visible, text sits on lines, red margin visible on left
- [ ] **`+` button:** Tap `+` on a block → bottom sheet appears with 4 tiles
- [ ] **Insert Text block:** New text block appears below, cursor moves to it
- [ ] **Insert Image block:** Gallery picker opens → selected images appear as grid in page
- [ ] **Insert Divider:** Gradient horizontal rule appears between blocks
- [ ] **Insert Checklist:** Checkbox row appears; type + press Next to add another item; check a box to see strikethrough
- [ ] **Format bubble:** Long-press / select text → dark bubble with B/I/U/bullet appears above selection
- [ ] **Save:** Press back or ✓ → entry saved, returns to list
- [ ] **Re-open entry:** All blocks and content reload correctly
- [ ] **Legacy entry:** Open a pre-existing entry (no `blocksJson`) → content appears as text block, images appear as image block; save → re-open confirms migration persisted
- [ ] **Empty entry guard:** Open new entry, type nothing, press back → no ghost entry in list
- [ ] **Image delete:** Long-press image block → confirmation dialog → block removed → save → re-open confirms deletion
