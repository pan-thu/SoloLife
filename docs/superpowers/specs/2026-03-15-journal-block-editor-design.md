# Journal Block Editor — Design Spec
**Date:** 2026-03-15
**Status:** Approved

---

## Overview

Replace the current `DiaryDetailScreen` (single `RichTextEditor` + bottom image strip) with a block-based notebook editor. The editor feels like a physical ruled-paper notebook: classic blue horizontal lines, a red left margin, and free-form content built from discrete blocks (text, image, divider, checklist).

---

## Goals

- Rich, OneNote-inspired editing experience on mobile
- Blocks can be inserted anywhere in the entry (not just appended at the end)
- Images display inline within the content flow, not in a separate strip
- Existing entries migrate transparently on first open — no data loss
- All-native Jetpack Compose implementation; no WebView or JS bridge

---

## Non-Goals

- Free-floating/draggable image placement (drag-to-any-x-y)
- Headings (H1/H2) block type (not in scope)
- Markdown shortcuts
- Sync or export

---

## Data Model

### `Block` (new file: `data/model/Block.kt`)

```kotlin
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

IDs are `UUID.randomUUID().toString()` at creation time.

### `DiaryEntry` changes

Add one column:
```kotlin
val blocksJson: String = ""   // JSON array of Block; "" = legacy entry
```

`content` and `imageUris` columns are kept. `content` is kept in sync (first text block's HTML) so the existing search DAO query (`LIKE '%query%'`) continues to work without changes. `imageUris` is kept in sync (all image paths from image blocks, pipe-joined) for backward compatibility.

### Database migration

`MIGRATION_2_3`: `ALTER TABLE diary_entries ADD COLUMN blocksJson TEXT NOT NULL DEFAULT ''`

DB version bumps from 2 → 3.

---

## Architecture

### `DiaryViewModel` additions

```kotlin
// Load entry; if blocksJson is blank, convert legacy content + imageUris into blocks
// and immediately re-save in the new format (transparent one-time migration per entry)
suspend fun getEntryAsBlocks(id: Long): Pair<String, List<Block>>

// Serialize blocks to JSON, derive content + imageUris for search/compat, persist
suspend fun saveBlocks(id: Long?, title: String, blocks: List<Block>, date: Long)
```

Both functions are `suspend` — they perform Room I/O and must be called from a coroutine scope (e.g. `scope.launch { vm.saveBlocks(...) }`).

`saveBlocks` derives `content` by concatenating the HTML of **all** `Text` blocks in order (joined with a newline), so search covers the full entry even when the first block is an image or checklist. It derives `imageUris` by collecting all `paths` from every `Image` block in order, pipe-joined.

The existing `save(id, title, content, date, imageUris)` is retained internally but no longer called from the UI directly.

### Editor local state (in `BlockEditorScreen`)

```kotlin
var blocks by remember { mutableStateOf<List<Block>>(emptyList()) }
val richTextStates = remember { mutableStateMapOf<String, RichTextState>() }
var focusedBlockId by remember { mutableStateOf<String?>(null) }
var showInsertMenuForBlockId by remember { mutableStateOf<String?>(null) }
var pendingDeletePaths by remember { mutableStateOf<List<String>>(emptyList()) }
```

Each `Text` block owns one `RichTextState` keyed by block ID. On save, each state's `.toHtml()` is written back into the block before calling `saveBlocks()`.

**`richTextStates` lifecycle:** when a `Text` block is deleted from `blocks`, its entry must also be removed from `richTextStates` (`richTextStates.remove(block.id)`) to prevent memory leaks over long editing sessions.

**Image delete staging:** when an image block is deleted (or individual paths removed), the paths are added to `pendingDeletePaths` — not deleted from disk immediately. After `saveBlocks()` completes successfully, each path in `pendingDeletePaths` is deleted via the existing `deleteImage()` util. This matches the existing pattern in `DiaryDetailScreen` and prevents data loss if the save fails.

---

## UI Components

### `NotePageBackground` (`ui/diary/NotePageBackground.kt`)

A `Canvas` composable that fills its parent and draws:
- Horizontal blue lines every **28dp** (`Color(0xFFB8D4E8)`, alpha 0.35)
- One vertical red line at **40dp** from the left edge (`Color(0xFFFFB3B3)`, alpha 0.4)

The first line is drawn at **y = 28dp** (one line-height from the top). The `LazyColumn` content must use a matching **top padding of 28dp** so that block text baselines sit on the ruled lines rather than between them.

Drawn behind all block content via `Box` layering.

### `BlockEditorScreen` (`ui/diary/BlockEditorScreen.kt`)

Replaces `DiaryDetailScreen`. Structure:

```
Scaffold
  topBar: date (tappable) + save + back
  content:
    Box(fillMaxSize) {
      NotePageBackground()
      LazyColumn(contentPadding left=48dp, right=16dp) {
        item { TitleField() }
        item { HorizontalDivider() }
        items(blocks, key = { it.id }) { block ->
          BlockRow(block, ...)   // each block + its "+" button
        }
      }
    }
```

Content left padding is **48dp** (right of the margin line). The `+` button is positioned absolutely in the 40dp margin area, vertically centered on each block row.

### Block composables (`ui/diary/blocks/`)

**`TextBlock.kt`**
- `RichTextEditor` with transparent container/indicator colors
- Placeholder: "Write something…" when empty
- `RichTextState` passed in, owned by the screen

**`ImageBlock.kt`**
- 1 image → full-width `AsyncImage` with `ContentScale.Crop`, rounded corners (8dp)
- 2+ images → `LazyVerticalGrid` with 2 columns, each cell square-cropped
- Tap-and-hold → show delete confirmation for the whole block

**`DividerBlock.kt`**
- Single `Box` with a gradient `drawBehind`: `transparent → Color(0xFFAAAAAA) → transparent`

**`ChecklistBlock.kt`**
- `Column` of rows; each row: `Checkbox` + `BasicTextField`
- Checked items: `TextDecoration.LineThrough` + 50% alpha text
- `ImeAction.Next` on each field appends a new `CheckItem` and moves focus

**`FormattingBubble.kt`** (`ui/diary/blocks/FormattingBubble.kt`)
- `Popup(alignment = Alignment.TopStart)` anchored above the focused `RichTextEditor`
- Shows: **B** / *I* / U̲ / bullet list toggle
- Visibility: shown when `focusedBlockId != null` and the `RichTextState` has a non-collapsed selection
- Dismissed on tap-outside or selection collapse
- The existing `FormattingToolbar` bottom bar from `DiaryDetailScreen` is **not carried over**. The bubble is the sole formatting UI in the new editor. The `Scaffold.bottomBar` slot is unused (no bottom toolbar).

**`BlockInsertMenu.kt`** (`ui/diary/blocks/BlockInsertMenu.kt`)
- `ModalBottomSheet`
- 2×2 grid of tiles: **Text** (📝) / **Image** (🖼️) / **Divider** (➖) / **Checklist** (☑️)
- Tapping a tile inserts the new block immediately after the block whose `+` button was tapped
- Image tile launches `ActivityResultContracts.GetMultipleContents("image/*")` — all selected images are saved via the existing `saveImage()` util and bundled into a **single** `Image` block (one `Image` block per picker session, with `paths` containing all selected files). This is consistent with the auto-grid rendering model.

---

## Empty-Entry Guard

On save (back button or checkmark), if `title` is blank **and** `blocks` is either empty or contains only a single `Text` block whose `RichTextState` text is blank → call `onBack()` without saving (matching the existing `DiaryDetailScreen` behaviour). This prevents ghost entries from appearing in the list.

## Block Insertion Flow

1. User taps `+` button on block row N → `showInsertMenuForBlockId = block.id`
2. `BlockInsertMenu` opens
3. User selects block type → new block created with fresh UUID
4. New block inserted into `blocks` list at index N+1
5. If Text block: a new `RichTextState` is added to `richTextStates` and focus is requested
6. `showInsertMenuForBlockId = null` → sheet dismisses

---

## Legacy Migration

On `getEntryAsBlocks(id)`:
1. Load `DiaryEntry` from Room
2. If `blocksJson` is blank:
   a. Create a `Text` block from `entry.content` (using `setHtml()` if starts with `<`, else `setText()`)
   b. If `entry.imageUris` is non-blank, create an `Image` block from `parseUris(entry.imageUris)`
   c. Call `saveBlocks()` immediately to persist the new format
   d. Return the converted blocks
3. If `blocksJson` is non-blank: deserialize and return directly

This is a one-time, transparent, per-entry migration triggered on first open.

---

## Navigation

`AppNavigation.kt`: routes `diary/new` and `diary/{entryId}` updated to instantiate `BlockEditorScreen` instead of `DiaryDetailScreen`.

`DiaryDetailScreen.kt` is deleted after `BlockEditorScreen` is wired up and verified.

---

## Files Summary

| Action | File |
|--------|------|
| Create | `data/model/Block.kt` |
| Create | `ui/diary/BlockEditorScreen.kt` |
| Create | `ui/diary/NotePageBackground.kt` |
| Create | `ui/diary/blocks/TextBlock.kt` |
| Create | `ui/diary/blocks/ImageBlock.kt` |
| Create | `ui/diary/blocks/DividerBlock.kt` |
| Create | `ui/diary/blocks/ChecklistBlock.kt` |
| Create | `ui/diary/blocks/FormattingBubble.kt` |
| Create | `ui/diary/blocks/BlockInsertMenu.kt` |
| Modify | `data/db/DiaryEntry.kt` — add `blocksJson` column |
| Modify | `data/db/SoloLifeDatabase.kt` — version 3, `MIGRATION_2_3` |
| Modify | `ui/diary/DiaryViewModel.kt` — add `saveBlocks`, `getEntryAsBlocks` |
| Modify | `navigation/AppNavigation.kt` — point routes to `BlockEditorScreen` |
| Delete | `ui/diary/DiaryDetailScreen.kt` — after new screen is wired up |
