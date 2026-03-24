# Journal Search Feature — Design Spec

**Date:** 2026-03-24
**Status:** Approved

---

## Overview

Add a keyword search bar to `DiaryListScreen` that lets users search journal entries by keyword — either across all entries or scoped to a date-filtered subset. The backend (DAO query, ViewModel logic) already exists; this spec covers the UI addition only.

---

## Architecture

### What already exists (no changes needed)

- `DiaryDao.search(query: String)` — LIKE search on `title` and `content` columns, returns `Flow<List<DiaryEntry>>`.
- `DiaryViewModel._query: MutableStateFlow<String>` and `setQuery(q: String)`.
- `DiaryViewModel.uiState` combines `_query` + `_selectedDate`: search is applied first via `flatMapLatest`, then date filter is applied to the result in-memory.
- `DiaryViewModel.clearDateFilter()` clears `_selectedDate`.

### New state

A single local UI boolean `searchActive: Boolean` (via `remember { mutableStateOf(false) }`) inside `DiaryListScreen`. This is purely view-level and does not belong in the ViewModel.

### Data flow when searching

1. User taps search icon → `searchActive = true`, keyboard opens.
2. User types → `vm.setQuery(q)` called on each keystroke.
3. ViewModel emits filtered entries via `uiState.entries`.
4. If a date filter is also active (`uiState.selectedDate != null`), entries are further narrowed to that day (existing logic, unchanged).
5. User taps X or back-arrow → `vm.setQuery("")`, `searchActive = false`, keyboard dismissed.

---

## Components

### `DiaryListScreen` (modified)

**New local state:**
```kotlin
var searchActive by rememberSaveable { mutableStateOf(false) }
```
(`rememberSaveable` ensures the search bar stays open if the user navigates to an entry and back — avoiding the state where the list is filtered but no search bar is visible.)

**TopAppBar — two animated states via `AnimatedContent(targetState = searchActive)`:**

- **`searchActive == false` (default):**
  - Title: "Diary"
  - Actions: streak badge (existing) + search icon (`Icons.Rounded.Search`) + calendar icon (existing)
  - Tapping search icon sets `searchActive = true`

- **`searchActive == true`:**
  - Title area replaced by full-width `TextField` (auto-focused via `FocusRequester` + `LaunchedEffect(searchActive)`, keyboard opens)
  - `TextField` value bound to `uiState.query` (from `DiaryListUiState`), not to a separate local string
  - Leading icon: back-arrow (`Icons.AutoMirrored.Rounded.ArrowBack`) — tapping calls `vm.setQuery("")`, `LocalFocusManager.clearFocus()`, and sets `searchActive = false`
  - Trailing icon: X (`Icons.Rounded.Close`) — visible only when `uiState.query` is non-empty, calls `vm.setQuery("")` only (bar stays open)
  - Streak badge and calendar icon hidden (bar is full width)
  - Opening search collapses the calendar panel (`calendarExpanded = false`) to avoid layout conflict

**Below the TopAppBar (unchanged):**
- Date filter chip remains visible when `selectedDate != null`, regardless of search state
- Calendar panel (AnimatedVisibility) unchanged

**Entry list (unchanged):**
- Renders `uiState.entries` as before
- Empty state (`DiaryEmptyState`) already handles zero results

---

## Interactions

| User action | Result |
|---|---|
| Tap search icon | `searchActive = true`, `calendarExpanded = false`, keyboard opens, field focused |
| Type in search bar | `vm.setQuery(q)` called, list updates reactively |
| Tap X (clear) in search bar | `vm.setQuery("")`, field stays open |
| Tap back-arrow in search bar | `vm.setQuery("")`, `searchActive = false`, keyboard dismissed |
| Active date filter + search | Both filters stack — search narrows first, then date filter narrows further |
| Clear date chip while searching | Date filter cleared, search results remain |
| Tap calendar icon (search inactive) | Calendar panel expands as before |

---

## Error / edge cases

- **Empty query while search bar open:** `_allEntries` is shown (no search applied) — existing ViewModel behaviour.
- **No results:** `DiaryEmptyState` composable is shown — existing behaviour, no special "no search results" state needed.
- **Back gesture while search active:** A `BackHandler(enabled = searchActive)` **must** be registered. Without it, the system back gesture will navigate away from the screen instead of collapsing the search bar (especially on API 33+ predictive back). The handler calls `vm.setQuery("")`, `focusManager.clearFocus()`, and sets `searchActive = false`. Note: if the keyboard is open, the system dismisses it on the first back press; the `BackHandler` fires on the second press — this two-step behaviour is expected and correct.
- **Calendar open when search activated:** Setting `searchActive = true` also sets `calendarExpanded = false`, so the calendar panel collapses automatically. The user can re-open the calendar after closing search.

## Implementation notes

- **Keyboard open on activation:** Place a `FocusRequester` on the `TextField`. Inside the `searchActive == true` branch of `AnimatedContent`, use `LaunchedEffect(Unit) { focusRequester.requestFocus() }` — scoped to that branch so it runs exactly once when the search bar is composed, not at screen level.
- **Keyboard dismiss on collapse:** Call `LocalFocusManager.current.clearFocus()` before setting `searchActive = false`.
- **TextField value:** Bind to `uiState.query` (part of `DiaryListUiState`, already collected) — do not maintain a separate local string state.

---

## Files changed

| File | Change |
|---|---|
| `ui/diary/DiaryListScreen.kt` | Add `searchActive` state, add search icon to TopAppBar actions, wrap TopAppBar content in `AnimatedContent`, add `TextField` for active state |

**No other files are modified.**

---

## Out of scope

- Search history / suggestions
- Highlighting matched keywords in entry previews
- Searching expense entries
- FTS (Full-Text Search) index — LIKE is sufficient for personal-scale data
