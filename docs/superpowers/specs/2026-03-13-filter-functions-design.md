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

### UI Behaviour
- `DiaryCalendarView` gains tap-on-date interaction. Only dates that have a dot (i.e. have entries) are tappable.
- Selected date renders a filled accent-coloured circle behind the day number.
- A dismissible `FilterChip` (e.g. `📅 Mar 10 ✕`) appears below the toggle row when a date is active. Tapping ✕ clears the filter.
- When the user switches to LIST view while a date filter is active, the list shows only that day's entries (filter persists across view mode switches).
- Tapping a different date replaces the current filter (single-date selection only, no range).

### Data Flow
- `DiaryListUiState` gains `selectedDate: LocalDate? = null`.
- `DiaryViewModel` gains:
  - `setSelectedDate(date: LocalDate)` — sets `_selectedDate`; clears if same date tapped again (toggle).
  - `clearDateFilter()` — sets `_selectedDate` to null.
- The combined flow adds `_selectedDate` as a fifth input; when non-null it filters `entries` to those whose `date` matches.
- `calendarEntryDates` is unaffected (always shows full month dots regardless of filter).

### ViewModel State Addition
```kotlin
// New field in DiaryListUiState
val selectedDate: LocalDate? = null

// New MutableStateFlow in DiaryViewModel
private val _selectedDate = MutableStateFlow<LocalDate?>(null)
```

---

## 3. Expense Date Filter

### Goal
A two-level drill-down (Month → Week) that filters the expense list, composing with the existing category filter.

### Sealed Class
```kotlin
sealed class ExpenseDateFilter {
    object None : ExpenseDateFilter()
    data class Month(val year: Int, val month: Int) : ExpenseDateFilter()
    data class Week(
        val year: Int,
        val month: Int,
        val weekIndex: Int,   // 0-based index within month
        val start: LocalDate,
        val end: LocalDate
    ) : ExpenseDateFilter()
}
```

### UI — Breadcrumb Strip
A horizontal strip sits between the screen header and the category breakdown bar. It always shows the active selection path:

| Level | Strip displays |
|-------|---------------|
| None | `[ All time ]` chip (muted, non-interactive) |
| Month | `[ ← All ]  [ March 2026 ✕ ]` |
| Week | `[ ← March 2026 ]  [ Week 2: Mar 10–16 ✕ ]` |

- `← All` / `← March 2026` are back-navigation chips.
- ✕ on the rightmost chip clears to the parent level (Week → Month, Month → None).

### UI — Level 0: Month Scroller (None state)
- A horizontal `LazyRow` of month chips spanning Jan–Dec of the current year, with current month pre-selected visually (bold, accent border).
- Tapping a month chip sets `ExpenseDateFilter.Month` and transitions to Level 1.
- Past months with no expenses are shown greyed out but still tappable.

### UI — Level 1: Week Cards (Month selected)
- The main list area is replaced by a vertical column of week summary cards.
- Each card shows: `Week N  ·  Mar 3–9  ·  ฿2,400` with a mini sparkline bar showing day distribution.
- Tapping a week card sets `ExpenseDateFilter.Week` and transitions to Level 2.

### UI — Level 2: Weekly Expense List (Week selected)
- The normal expense list filtered to the selected week, grouped by day using the existing `DayHeader` + `ExpenseRow` pattern.
- Category filter composes on top (existing behaviour).
- No day-level selection — the grouped list is the terminal view.

### Transitions
- `AnimatedContent` with `slideInHorizontally` / `slideOutHorizontally` shared-axis animation.
- Drilling down slides content left; going back slides right.

### Data Flow
- `ExpensesUiState` gains `dateFilter: ExpenseDateFilter = ExpenseDateFilter.None`.
- `ExpensesViewModel` gains:
  - `setDateFilter(filter: ExpenseDateFilter)` — replaces current filter.
  - `clearDateFilter()` — resets to `None`.
- Combined flow filters `expenses`:
  1. `None` → all expenses.
  2. `Month` → expenses where `date` falls in `year/month`.
  3. `Week` → expenses where `date` in `start..end`.
- `weekTotal` in state is repurposed / supplemented: when a `Week` filter is active, show total for that week; when `Month` is active, show month total.
- Week card data (total per week, day distribution) is computed in the ViewModel from `allExpenses` filtered to the selected month.

---

## 4. Files to Create / Modify

| File | Change |
|------|--------|
| `ui/diary/DiaryViewModel.kt` | Add `_selectedDate` flow, `setSelectedDate()`, `clearDateFilter()`, update combine |
| `ui/diary/DiaryListScreen.kt` | Make calendar dates tappable, add `FilterChip`, pass `selectedDate` to calendar |
| `ui/expenses/ExpensesViewModel.kt` | Add `ExpenseDateFilter` sealed class, `_dateFilter` flow, filter logic, week summary computation |
| `ui/expenses/ExpensesScreen.kt` | Add breadcrumb strip, month scroller, week cards, `AnimatedContent` transitions |

No new files required — all changes extend existing components.

---

## 5. Out of Scope

- Multi-date or date-range selection for diary.
- Year-level navigation for expenses (current year only).
- Persistence of filter state across app restarts.
- Day-level drill-down for expenses.
