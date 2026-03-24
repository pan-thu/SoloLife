# Journal Search Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an expanding keyword search bar to the Diary screen's top bar that filters journal entries in real time, combining with the existing date filter.

**Architecture:** All changes are confined to a single file (`DiaryListScreen.kt`). The ViewModel already handles search + date filter stacking via `_query` + `_selectedDate` StateFlows — no backend changes needed. The TopAppBar is wrapped in `AnimatedContent` to swap between a normal title/actions row and a full-width search `TextField`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room (no new dependencies)

---

## File Map

| File | Change |
|---|---|
| `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt` | Only file changed — add state, imports, BackHandler, and AnimatedContent TopAppBar |

---

## Reference: Existing code to know before starting

Read these before touching anything:

- `DiaryListScreen.kt` — the only file you'll edit. Note: `TopAppBar` has `title` and `actions` slots. The screen already has `calendarExpanded` local state (Boolean). `state` is `DiaryListUiState` collected from `vm.uiState`.
- `DiaryViewModel.kt` — `vm.setQuery(q: String)` sets the search query; `uiState.query: String` holds the current query value. No changes to this file.
- `DiaryListUiState` (top of `DiaryViewModel.kt`) — has `query: String` field already.

---

## Task 1: Add state, focusManager, collapseSearch, and BackHandler

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt`

This task adds the scaffolding that the rest of the feature hangs on. No visible UI change yet.

- [ ] **Step 1: Add missing imports**

Open `DiaryListScreen.kt`. Add these imports (alongside the existing imports block):

```kotlin
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.rounded.Search
```

- [ ] **Step 2: Add `searchActive` state and `focusManager` inside `DiaryListScreen`**

Inside `DiaryListScreen`, directly after the existing `var calendarExpanded` declaration (around line 55), add:

```kotlin
var searchActive by rememberSaveable { mutableStateOf(false) }
val focusManager = LocalFocusManager.current
```

- [ ] **Step 3: Add `collapseSearch` lambda**

Directly after the `focusManager` line, add:

```kotlin
val collapseSearch = {
    vm.setQuery("")
    focusManager.clearFocus()
    searchActive = false
}
```

- [ ] **Step 4: Register `BackHandler`**

After the `collapseSearch` lambda (still inside `DiaryListScreen`, before the `Box`/`Scaffold`), add:

```kotlin
BackHandler(enabled = searchActive) {
    collapseSearch()
}
```

**Why:** On API 33+ with predictive back, without this the system back gesture navigates away from the screen rather than collapsing the search bar. The handler fires after the system has already dismissed the keyboard on the first back press — that two-step behaviour is expected.

- [ ] **Step 5: Build to verify no compile errors**

```bash
cd /data/projects/SoloLife
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt
git commit -m "feat(search): add searchActive state, focusManager, BackHandler scaffold"
```

---

## Task 2: Add search icon to the normal TopAppBar and activate search on tap

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt`

Adds the search icon to the existing (non-search) top bar. Tapping it activates search and collapses the calendar.

- [ ] **Step 1: Add search icon button to TopAppBar actions**

In the `TopAppBar`'s `actions` block, add a search `IconButton` **before** the existing calendar `IconButton`:

```kotlin
// Search icon — opens search bar
IconButton(onClick = {
    searchActive = true
    calendarExpanded = false
}) {
    Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = "Search entries"
    )
}
// Calendar toggle (existing — keep as-is)
IconButton(onClick = {
    calendarExpanded = !calendarExpanded
    if (!calendarExpanded) pendingDate = null
}) {
    Icon(
        imageVector = Icons.Rounded.CalendarMonth,
        contentDescription = "Toggle calendar"
    )
}
```

- [ ] **Step 2: Build and install on device/emulator**

```bash
./gradlew installDebug 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, app installs.

- [ ] **Step 3: Manual verify**

Open the Diary screen. Confirm:
- A search icon (magnifying glass) appears to the left of the calendar icon in the top bar.
- Tapping it sets `searchActive = true` (no visible change yet since `AnimatedContent` not wired, but this will be verified in Task 3).
- The streak badge still shows correctly.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt
git commit -m "feat(search): add search icon to diary top bar"
```

---

## Task 3: Wrap TopAppBar in AnimatedContent and add search-active state UI

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt`

This is the core UI task. Replaces the single `TopAppBar` with an `AnimatedContent`-wrapped pair — one for each `searchActive` state.

- [ ] **Step 1: Wrap the existing TopAppBar in `AnimatedContent`**

The current `topBar` slot in `Scaffold` looks like:

```kotlin
topBar = {
    TopAppBar(
        title = { Text("Diary", ...) },
        ...
        actions = { ... }
    )
},
```

Replace the entire `topBar = { ... }` block with:

```kotlin
topBar = {
    AnimatedContent(
        targetState = searchActive,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "DiaryTopBarToggle"
    ) { active ->
        if (!active) {
            // ── NORMAL STATE ──────────────────────────────────────────────
            TopAppBar(
                title = { Text("Diary", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    // Streak badge (existing)
                    if (state.currentStreak > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    state.currentStreak.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    // Search icon
                    IconButton(onClick = {
                        searchActive = true
                        calendarExpanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search entries"
                        )
                    }
                    // Calendar toggle
                    IconButton(onClick = {
                        calendarExpanded = !calendarExpanded
                        if (!calendarExpanded) pendingDate = null
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Toggle calendar"
                        )
                    }
                }
            )
        } else {
            // ── SEARCH ACTIVE STATE ───────────────────────────────────────
            val focusRequester = remember { FocusRequester() }
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = collapseSearch) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Close search"
                        )
                    }
                },
                title = {
                    BasicTextField(
                        value = state.query,
                        onValueChange = { vm.setQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (state.query.isEmpty()) {
                                Text(
                                    text = "Search entries…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                },
                actions = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    }
},
```

**Note:** `Icons.Rounded.ArrowBack` is used (not `AutoMirrored`) — fine for this LTR app.

**Note:** The `LaunchedEffect(Unit)` is scoped **inside** the `active == true` branch of `AnimatedContent`, so it runs exactly once when the search bar is first composed. Do not move it to screen level.

- [ ] **Step 2: Remove the now-duplicate search icon and calendar toggle from earlier**

After the AnimatedContent refactor, the `topBar` slot contains the full TopAppBar for both states. If you added the search icon in Task 2 to the existing `TopAppBar` (which is now replaced), those additions are automatically replaced — just verify the new code above is the only TopAppBar definition.

- [ ] **Step 3: Build**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Install and manually test the full search flow**

```bash
./gradlew installDebug
```

Test each scenario:

| Scenario | Expected |
|---|---|
| Open Diary screen | Normal top bar shows: "Diary" title, streak badge (if any), search icon, calendar icon |
| Tap search icon | Top bar fades to search state: back-arrow + text field + no calendar icon. Keyboard opens automatically. |
| Type a keyword | Entry list filters in real time to entries whose title or content contains the keyword |
| Tap X (when text entered) | Query cleared, field stays open, all entries shown again |
| Tap back-arrow | Query cleared, keyboard dismissed, top bar fades back to normal state |
| System back gesture (search open, keyboard dismissed) | Top bar collapses back to normal |
| Open calendar (search inactive), then tap search icon | Calendar collapses, search bar opens |
| Tap calendar icon to pick a date, then search | Date chip visible + search field active — list is narrowed to that day's entries matching the keyword |
| Clear date chip while search active | Date filter cleared, full search results shown |

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt
git commit -m "feat(search): animated expanding search bar in diary top bar"
```
