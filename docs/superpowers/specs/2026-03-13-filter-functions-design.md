# Filter Functions — Diary & Expenses

**Date:** 2026-03-13
**Status:** Approved
**Scope:** DiaryListScreen + DiaryViewModel, ExpensesScreen + ExpensesViewModel

---

## 1. Overview

Add date filtering to the Diary screen and a two-level date drill-down (Month → Week) plus category filtering to the Expenses screen. Both features extend existing UI patterns rather than introducing new surfaces.

---

## 2. Diary Date Filter

### Goal
Let users tap a date on the existing calendar view to filter the list to that day's entries.

### Limitation
The calendar always shows the current month. Filtering to past-month dates is **intentionally out of scope** — no month navigation is added.

### `DiaryCalendarView` Signature Change
**`currentMonthStart: Long` stays unchanged** — `DateUtils.formatMonth` and the internal `Instant.ofEpochMilli` call both depend on it. Only `onDayClick` changes type: `(Long) → (LocalDate)`. Update the single call site in `DiaryListScreen` accordingly (currently passes day-start millis; change to pass the `LocalDate` computed inside the calendar). Add a new optional parameter:
```kotlin
@Composable
fun DiaryCalendarView(
    currentMonthStart: Long,              // unchanged
    entryDates: Set<Long>,
    selectedDate: LocalDate? = null,      // NEW
    onDayClick: (LocalDate) -> Unit,      // type changed: Long → LocalDate
)
```
- When `selectedDate` is non-null and falls in the visible month, that day cell renders a filled accent-coloured circle behind the day number.
- The existing tap-guard (`if (hasEntry) Modifier.clickable { onDayClick(date) }`) is **kept**. A selected day can be tapped again to deselect — which still satisfies the guard since it has an entry (that's why it was selected). The toggle-off logic lives in `setSelectedDate` in the ViewModel, not in the calendar.

### UI Behaviour
- A dismissible `FilterChip` with a leading `Icons.Rounded.CalendarToday` icon, label `Mar 10`, and a trailing icon `Icons.Rounded.Close` whose `onClick = { vm.clearDateFilter() }` is added in the following position: the existing screen content inside the `padding Box` begins with a `when` on load state and view mode. Wrap all of this in a new `Column` so the chip can be placed as the first child:

  ```kotlin
  Box(modifier = Modifier.padding(innerPadding)) {
      Column {
          // FilterChip: always rendered when selectedDate != null,
          // even during shimmer loading or empty state
          if (state.selectedDate != null) { FilterChip(...) }

          when {
              !state.isLoaded -> { /* shimmer */ }
              state.viewMode == DiaryViewMode.CALENDAR -> { /* calendar */ }
              state.entries.isEmpty() -> { DiaryEmptyState() }
              else -> { /* list */ }
          }
      }
  }
  ```

- If the filtered list is empty (no entries on the selected date), `DiaryEmptyState()` is shown — the chip remains visible above it.
- Filter persists across view mode switches. When switching to LIST with an active filter, filtered entries appear from the top of the list; no auto-scroll.
- Tapping a different date replaces the filter. Tapping the same date again calls `setSelectedDate` → toggles off.
- **The existing `onOpenEntry` navigation on calendar day tap is removed.** Tap exclusively calls `setSelectedDate`. Entry navigation remains via list rows.

### Data Flow
- `DiaryListUiState` gains `selectedDate: LocalDate? = null`.
- `DiaryViewModel` gains:
  - `private val _selectedDate = MutableStateFlow<LocalDate?>(null)`.
  - `setSelectedDate(date: LocalDate)` — toggles off if same date, otherwise sets.
  - `clearDateFilter()` — sets `_selectedDate` to `null`.
- The existing `combine` takes 4 flows; adding `_selectedDate` as the 5th uses the **5-argument overload**. The project pulls coroutines transitively via Room 2.7.0 and lifecycle-viewmodel-compose 2.9.0, both of which require kotlinx-coroutines ≥ 1.7.x. The 5-arg overload is available ≥ 1.6.0, so no version issue. `java.time` APIs used throughout this spec are native on minSdk 35 — no desugaring required.
  1. `_query.flatMapLatest { ... }` → `filteredEntries`
  2. `_viewMode` → `viewMode`
  3. `repo.getEntryDatesInRange(...)` → `monthDates`
  4. `_allEntries` → `allEntries`
  5. `_selectedDate` → `selectedDate`

- Date filtering is applied in-memory in the lambda as a post-filter over `filteredEntries`. Not a Room query.
- Use `DateUtils.isSameDay(entry.date, dayMillis)` — same contract used throughout `DiaryListScreen`. Convert `selectedDate`: `selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()`.
- `calendarEntryDates` (from `monthDates`) is unaffected.

---

## 3. Expense Date Filter

### Goal
A two-level drill-down (Month → Week) that filters the expense list, composing with the existing category filter.

### `ExpenseDateFilter` Sealed Class
Top-level in `ExpensesViewModel.kt`:

```kotlin
sealed class ExpenseDateFilter {
    object None : ExpenseDateFilter()
    data class Month(val year: Int, val month: Int) : ExpenseDateFilter()
    data class Week(
        val year: Int,
        val month: Int,
        val weekIndex: Int,   // 0-based index within month
        val start: LocalDate, // full ISO week Monday — may precede month start
        val end: LocalDate    // full ISO week Sunday — may follow month end
    ) : ExpenseDateFilter()
}
```

`start`/`end` store the full ISO range. Filtering uses the full range. The UI display label clamps to the month boundaries.

### Week-Splitting Algorithm
1. `monthStart = LocalDate(year, month, 1)`
2. `weekStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))`
3. While `weekStart < monthStart.plusMonths(1)`: record `[weekStart, weekStart+6]`; `weekStart += 7`.
4. `weekIndex` is 0-based; `start`/`end` are unclamped.

### `WeekSummary` Data Class
Top-level in `ExpensesViewModel.kt`:

```kotlin
data class WeekSummary(
    val filter: ExpenseDateFilter.Week,
    val total: Double,
    val dailyTotals: List<Double> // 7 entries, Mon=0…Sun=6
)
```

### `ExpensesUiState` Changes
- `weekTotal: Double` → **renamed `displayTotal: Double` (default `0.0`)**.
- New: `weekSummaries: List<WeekSummary> = emptyList()`.
- New: `dateFilter: ExpenseDateFilter = ExpenseDateFilter.None`.

| Filter | `displayTotal` | Rendered? |
|---|---|---|
| `None` | `0.0` | **No** |
| `Month` | Sum of expenses in selected month | Yes, in month chip |
| `Week` | Sum of expenses in `start..end` | Yes, in week chip |

**`displayTotal` is intentionally category-unfiltered** — it reflects total period spending regardless of active category chip. The total in the breadcrumb may not equal the sum of visible rows when both filters are active. This is by design. The existing `DayHeader` per-day subtotals continue to be derived from the category-filtered list, so they reflect visible rows. The breadcrumb total and day-level totals intentionally use different bases.

`allExpenses` continues to hold the full unfiltered dataset (for `CategoryBreakdownBar`, which always shows proportions across all time).

### ViewModel Changes

**`combine` restructure — 4 inputs (replacing `repo.weekTotal()` with `_dateFilter`):**
1. `repo.getAll()` → `allExpenses`
2. `_filterCategory` → `filterCat`
3. `_editingExpense` → `editingExpense`
4. `_dateFilter` → `dateFilter`

Removing `repo.weekTotal()` also eliminates a latent staleness bug: its `weekStart`/`weekEnd` arguments were snapshot values computed at ViewModel construction time. The DAO method itself may remain or be deleted — it is no longer called.

**Inside the combine lambda:**

DST-safe date extraction (local extension or helper):
```kotlin
fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
```

Step 1 — **`dateFilteredExpenses`** (intermediate, named explicitly):
```kotlin
val dateFilteredExpenses = when (dateFilter) {
    is None -> allExpenses
    is Month -> allExpenses.filter {
        val ld = it.date.toLocalDate()
        ld.year == dateFilter.year && ld.monthValue == dateFilter.month
    }
    is Week -> allExpenses.filter {
        it.date.toLocalDate() in dateFilter.start..dateFilter.end
    }
}
```

Step 2 — apply category filter on top of `dateFilteredExpenses` (existing logic).

Step 3 — **`displayTotal`**:
- `None` → `0.0`
- `Month`/`Week` → `dateFilteredExpenses.sumOf { it.amount }`

Step 4 — **`weekSummaries`** (only when `dateFilter is Month`; else `emptyList()`):
- Run week-splitting for selected month.
- For each week `w`:
  - Filter `allExpenses` to `w.start..w.end`.
  - `total` = sum; `dailyTotals` = 7-element list indexed by `DayOfWeek.value - 1`.

**New ViewModel API:**
- `private val _dateFilter = MutableStateFlow<ExpenseDateFilter>(ExpenseDateFilter.None)`
- `fun setDateFilter(filter: ExpenseDateFilter)`.
- `fun clearDateFilter()` → sets to `None`.

### UI — `WeekSummaryCard` Removal
`WeekSummaryCard` composable is **deleted** along with its call site. `state.weekTotal` no longer exists.

### UI — Empty State Under Active Filters
When a `Month` or `Week` filter yields zero visible expenses: show `ExpensesEmptyState()` in the list area. The breadcrumb strip remains visible above it so the user can navigate back or clear.

### UI — Breadcrumb Strip
Replaces `WeekSummaryCard` at the top of screen content (below screen top bar):

| Filter | Content |
|---|---|
| `None` | `LazyRow` of month chips, Jan–Dec current year |
| `Month` | `[ ← All ]  [ March 2026 · ฿X,XXX  ✕ ]` |
| `Week` | `[ ← March 2026 ]  [ Week 2: Mar 10–16 · ฿X,XXX  ✕ ]` |

`displayTotal` rendered **only** for `Month` and `Week` states. `← All` calls `clearDateFilter()`; `← March` calls `setDateFilter(Month(...))`.

### UI — Level 0: Month Scroller (`None`)
- `LazyRow`, Jan–Dec current year, current month highlighted.
- Months with no expenses greyed out but tappable.
- **Current year only — intentional.** No year navigation.
- Tapping calls `setDateFilter(Month(year, month))`.

### UI — Level 1: Week Cards (`Month`)
- `Column` of `WeekCard` composables inside `AnimatedContent`.
- Each card: `Week N  ·  [clamped dates]  ·  ฿X,XXX` + proportional bar: a `Row` of 7 `Box` elements (Mon–Sun). Max bar height = `24.dp`. Each `Box` height = `max(1.dp, (dailyTotals[i] / maxDailyTotal).coerceIn(0f, 1f) * 24.dp)` where `maxDailyTotal = dailyTotals.max()`. If `maxDailyTotal == 0.0`, all bars show the 1dp stub. Bars are vertically bottom-aligned within the row.
- Tapping calls `setDateFilter(Week(...))`.

### UI — Level 2: Weekly Expense List (`Week`)
- Existing `DayHeader` + `ExpenseRow` grouped list filtered to selected week.
- Category filter composes on top.
- Terminal view.

### Transitions
Use a plain `IntArray` ref (not `mutableIntStateOf`) so updating it never triggers recomposition:

```kotlin
val prevDepthRef = remember { intArrayOf(0) }  // plain ref, no recomposition on write
val currentDepth = when (state.dateFilter) {
    is ExpenseDateFilter.None -> 0
    is ExpenseDateFilter.Month -> 1
    is ExpenseDateFilter.Week -> 2
}
val goingDeeper = currentDepth > prevDepthRef[0]
SideEffect { prevDepthRef[0] = currentDepth }  // safe: no state read, no recomposition

AnimatedContent(
    targetState = state.dateFilter,
    transitionSpec = {
        slideInHorizontally { if (goingDeeper) it else -it } togetherWith
        slideOutHorizontally { if (goingDeeper) -it else it }
    }
) { filter -> ... }
```

Using `intArrayOf` (not `mutableIntStateOf`) means Compose does not subscribe to it, so the `SideEffect` write does not schedule an extra recomposition.

---

## 4. Files to Modify

| File | Change |
|------|--------|
| `ui/diary/DiaryViewModel.kt` | Add `_selectedDate`; `setSelectedDate()`, `clearDateFilter()`; 5-arg `combine`; in-memory date post-filter |
| `ui/diary/DiaryListScreen.kt` | Add `selectedDate` param to `DiaryCalendarView`; wrap content in `Column`; add `FilterChip`; remove `onOpenEntry` on calendar day tap |
| `ui/expenses/ExpensesViewModel.kt` | Add `ExpenseDateFilter`, `WeekSummary`; `_dateFilter` flow; remove `repo.weekTotal()` call; rename `weekTotal`→`displayTotal`; add `weekSummaries`, `dateFilter` to state; restructure combine with named `dateFilteredExpenses` |
| `ui/expenses/ExpensesScreen.kt` | Delete `WeekSummaryCard` composable + call site; add breadcrumb strip; month scroller; `WeekCard` composable; `AnimatedContent` with `SideEffect`-tracked direction |

No new files required.

---

## 5. Out of Scope

- Multi-date or date-range selection for diary.
- Past-month navigation in the diary calendar (current month only — intentional).
- Year-level navigation for expenses (current year only — intentional).
- Persistence of filter state across app restarts.
- Day-level drill-down for expenses.
- Animated number transition on `displayTotal` when filter changes.
