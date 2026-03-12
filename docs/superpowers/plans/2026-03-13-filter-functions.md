# Filter Functions Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a date filter to the Diary screen (tap calendar day to filter list) and a two-level Month→Week date drill-down with animated transitions to the Expenses screen.

**Architecture:** Both features extend existing ViewModel StateFlow combines with in-memory post-filters; no new Room queries. The Expenses screen replaces `WeekSummaryCard` with a `BreadcrumbStrip` composable that drives `AnimatedContent` between three drill-down levels.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, kotlinx-coroutines `combine`, `java.time` (native on minSdk 35), `AnimatedContent` + `SideEffect` for transitions.

---

## Chunk 1: Diary ViewModel

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryViewModel.kt`

---

- [ ] **Step 1: Add `selectedDate` to `DiaryListUiState`**

In `DiaryViewModel.kt`, update the data class — add `selectedDate` after `currentStreak`:

```kotlin
data class DiaryListUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val query: String = "",
    val isLoaded: Boolean = false,
    val viewMode: DiaryViewMode = DiaryViewMode.LIST,
    val calendarEntryDates: Set<Long> = emptySet(),
    val currentStreak: Int = 0,
    val selectedDate: java.time.LocalDate? = null   // NEW
)
```

- [ ] **Step 2: Add `_selectedDate` flow and its public API**

Inside `DiaryViewModel`, after `private val _viewMode = ...`, add:

```kotlin
private val _selectedDate = MutableStateFlow<java.time.LocalDate?>(null)

fun setSelectedDate(date: java.time.LocalDate) {
    _selectedDate.value = if (_selectedDate.value == date) null else date
}

fun clearDateFilter() {
    _selectedDate.value = null
}
```

- [ ] **Step 3: Migrate `combine` to 5-argument overload and add in-memory date filter**

Replace the entire existing `uiState` combine block with:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
val uiState: StateFlow<DiaryListUiState> = combine(
    _query.flatMapLatest { q ->
        if (q.isBlank()) _allEntries else repo.search(q)
    },
    _viewMode,
    run {
        repo.getEntryDatesInRange(currentMonthStart(), currentMonthEnd())
    },
    _allEntries,
    _selectedDate
) { filteredEntries, viewMode, monthDates, allEntries, selectedDate ->
    val entryDaySet = monthDates.map { DateUtils.dayStart(it) }.toSet()
    val streak = DateUtils.currentStreak(allEntries.map { it.date })
    val displayEntries = if (selectedDate == null) filteredEntries else {
        val zone = java.time.ZoneId.systemDefault()
        val dayMillis = selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()
        filteredEntries.filter { DateUtils.isSameDay(it.date, dayMillis) }
    }
    DiaryListUiState(
        entries = displayEntries,
        query = _query.value,
        isLoaded = true,
        viewMode = viewMode,
        calendarEntryDates = entryDaySet,
        currentStreak = streak,
        selectedDate = selectedDate
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryListUiState())
```

- [ ] **Step 4: Verify the project still compiles**

```bash
cd /data/projects/SoloLife && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with no errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/DiaryViewModel.kt
git commit -m "feat(diary): add selectedDate filter to DiaryViewModel"
```

---

## Chunk 2: Diary List Screen

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt`

---

- [ ] **Step 1: Add new imports**

At the top of `DiaryListScreen.kt`, add **only these two** (the rest are already covered by existing wildcard imports `androidx.compose.foundation.layout.*`, `androidx.compose.material3.*`, `androidx.compose.runtime.*`):

```kotlin
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
```

- [ ] **Step 2: Update `DiaryCalendarView` signature**

Change the signature of `DiaryCalendarView` (line 270–273) from:

```kotlin
private fun DiaryCalendarView(
    currentMonthStart: Long,
    entryDates: Set<Long>,
    onDayClick: (Long) -> Unit
) {
```

to:

```kotlin
private fun DiaryCalendarView(
    currentMonthStart: Long,
    entryDates: Set<Long>,
    selectedDate: LocalDate? = null,
    onDayClick: (LocalDate) -> Unit
) {
```

- [ ] **Step 3: Update `DiaryCalendarView` day-cell rendering**

Inside the `items(daysInMonth)` block in `DiaryCalendarView`, each iteration already declares `val dayDate`, `val dayMillis`, `val hasEntry`, `val isToday`. Add `val isSelected` **on the line immediately after `val isToday`** (before the `Box(...)` call):

```kotlin
val isSelected = selectedDate == dayDate
```

Then replace the `.background(if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)` modifier inside the `Box` with:

```kotlin
.background(
    when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else       -> Color.Transparent
    }
)
```

Also update the `Text` color for the day number (inside the `Column` inside the `Box`) from:

```kotlin
color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
```

to:

```kotlin
color = when {
    isSelected -> MaterialTheme.colorScheme.onPrimary
    isToday    -> MaterialTheme.colorScheme.primary
    else       -> MaterialTheme.colorScheme.onSurface
}
```

- [ ] **Step 4: Update the `onDayClick` lambda inside `DiaryCalendarView`**

The existing clickable lambda passes `dayMillis`:

```kotlin
if (hasEntry) Modifier.clickable { onDayClick(dayMillis) } else Modifier
```

Change to pass `dayDate` (a `LocalDate`):

```kotlin
if (hasEntry) Modifier.clickable { onDayClick(dayDate) } else Modifier
```

- [ ] **Step 5: Wrap the main screen content in a `Column` and add the `FilterChip`**

In `DiaryListScreen`, the `Scaffold` content currently starts with:

```kotlin
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
```

Replace the `Box { when { ... } }` block with a `Column` wrapper:

```kotlin
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Date filter chip
                if (state.selectedDate != null) {
                    val label = state.selectedDate.format(
                        java.time.format.DateTimeFormatter.ofPattern("MMM d")
                    )
                    FilterChip(
                        selected = true,
                        onClick = { vm.clearDateFilter() },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                    )
                }

                when {
                    !state.isLoaded -> {
                        LazyColumn {
                            items(5) { ShimmerDiaryCard() }
                        }
                    }
                    state.viewMode == DiaryViewMode.CALENDAR -> {
                        val zone = ZoneId.systemDefault()
                        val monthStart = LocalDate.now(zone).withDayOfMonth(1)
                            .atStartOfDay(zone).toInstant().toEpochMilli()
                        LazyColumn {
                            item {
                                DiaryCalendarView(
                                    currentMonthStart = monthStart,
                                    entryDates = state.calendarEntryDates,
                                    selectedDate = state.selectedDate,
                                    onDayClick = { date -> vm.setSelectedDate(date) }
                                )
                            }
                        }
                    }
                    state.entries.isEmpty() -> {
                        DiaryEmptyState()
                    }
                    else -> {
                        val grouped = remember(state.entries) { state.entries.groupByMonth() }
                        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                            grouped.forEach { (monthKey, entries) ->
                                item(key = "header_$monthKey") {
                                    PillMonthHeader(millis = monthKey)
                                }
                                itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                                    SwipeActionsContainer(
                                        item = entry,
                                        onDelete = { vm.delete(entry) },
                                        onEdit = { onOpenEntry(it.id) }
                                    ) {
                                        DiaryTimelineItem(
                                            entry = entry,
                                            isLast = index == entries.lastIndex,
                                            onClick = { onOpenEntry(entry.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
```

Note: The old `onOpenEntry` call inside the calendar day click is **removed**. The new `onDayClick` only calls `vm.setSelectedDate(date)`.

- [ ] **Step 6: Verify the project compiles**

```bash
cd /data/projects/SoloLife && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/diary/DiaryListScreen.kt
git commit -m "feat(diary): add date filter chip and tappable calendar"
```

---

## Chunk 3: Expenses ViewModel

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/expenses/ExpensesViewModel.kt`

---

- [ ] **Step 1: Add imports and `ExpenseDateFilter` / `WeekSummary` declarations**

**1a — Add imports** at the top of `ExpensesViewModel.kt`, in the import section (before the first class declaration):

```kotlin
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
```

**1b — Add sealed class and data class** immediately before the `ExpensesUiState` data class (not in the import section):

```kotlin
sealed class ExpenseDateFilter {
    object None : ExpenseDateFilter()
    data class Month(val year: Int, val month: Int) : ExpenseDateFilter()
    data class Week(
        val year: Int,
        val month: Int,
        val weekIndex: Int,
        val start: LocalDate,
        val end: LocalDate
    ) : ExpenseDateFilter()
}

data class WeekSummary(
    val filter: ExpenseDateFilter.Week,
    val total: Double,
    val dailyTotals: List<Double>  // 7 entries, Mon=0…Sun=6
)
```

- [ ] **Step 2: Update `ExpensesUiState`**

Replace the existing `ExpensesUiState` data class:

```kotlin
data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val allExpenses: List<Expense> = emptyList(),
    val displayTotal: Double = 0.0,          // renamed from weekTotal
    val filterCategory: ExpenseCategory? = null,
    val isLoaded: Boolean = false,
    val editingExpense: Expense? = null,
    val weekSummaries: List<WeekSummary> = emptyList(),
    val dateFilter: ExpenseDateFilter = ExpenseDateFilter.None
)
```

- [ ] **Step 3: Add `_dateFilter` flow and public API inside `ExpensesViewModel`**

After `private val _editingExpense = ...`, add:

```kotlin
private val _dateFilter = MutableStateFlow<ExpenseDateFilter>(ExpenseDateFilter.None)

fun setDateFilter(filter: ExpenseDateFilter) {
    _dateFilter.value = filter
}

fun clearDateFilter() {
    _dateFilter.value = ExpenseDateFilter.None
}
```

- [ ] **Step 4: Add the `Long.toLocalDate()` helper and `weeksForMonth()` utility**

After the `clearDateFilter()` function, add:

```kotlin
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun weeksForMonth(year: Int, month: Int): List<ExpenseDateFilter.Week> {
    val monthStart = LocalDate.of(year, month, 1)
    var weekStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val nextMonth = monthStart.plusMonths(1)
    val weeks = mutableListOf<ExpenseDateFilter.Week>()
    var index = 0
    while (weekStart < nextMonth) {
        val weekEnd = weekStart.plusDays(6)
        weeks += ExpenseDateFilter.Week(
            year = year,
            month = month,
            weekIndex = index++,
            start = weekStart,
            end = weekEnd
        )
        weekStart = weekStart.plusDays(7)
    }
    return weeks
}
```

- [ ] **Step 5: Replace the `combine` block**

Replace the entire existing `val uiState: StateFlow<ExpensesUiState> = combine(...)` block with:

```kotlin
val uiState: StateFlow<ExpensesUiState> = combine(
    repo.getAll(),
    _filterCategory,
    _editingExpense,
    _dateFilter
) { allExpenses, filterCat, editing, dateFilter ->
    // Step 1: date filter
    val dateFilteredExpenses = when (dateFilter) {
        is ExpenseDateFilter.None -> allExpenses
        is ExpenseDateFilter.Month -> allExpenses.filter {
            val ld = it.date.toLocalDate()
            ld.year == dateFilter.year && ld.monthValue == dateFilter.month
        }
        is ExpenseDateFilter.Week -> allExpenses.filter {
            it.date.toLocalDate() in dateFilter.start..dateFilter.end
        }
    }

    // Step 2: category filter
    val filtered = if (filterCat == null) dateFilteredExpenses
                   else dateFilteredExpenses.filter { it.category == filterCat.name }

    // Step 3: displayTotal (category-unfiltered; breadcrumb total)
    val displayTotal = when (dateFilter) {
        is ExpenseDateFilter.None -> 0.0
        else -> dateFilteredExpenses.sumOf { it.amount }
    }

    // Step 4: weekSummaries (only when drilling into a month)
    val weekSummaries = if (dateFilter is ExpenseDateFilter.Month) {
        weeksForMonth(dateFilter.year, dateFilter.month).map { week ->
            val weekExpenses = allExpenses.filter {
                it.date.toLocalDate() in week.start..week.end
            }
            val dailyTotals = List(7) { dayIdx ->
                val targetDay = week.start.plusDays(dayIdx.toLong())
                weekExpenses.filter { it.date.toLocalDate() == targetDay }.sumOf { it.amount }
            }
            WeekSummary(filter = week, total = weekExpenses.sumOf { it.amount }, dailyTotals = dailyTotals)
        }
    } else emptyList()

    ExpensesUiState(
        expenses = filtered,
        allExpenses = allExpenses,
        displayTotal = displayTotal,
        filterCategory = filterCat,
        isLoaded = true,
        editingExpense = editing,
        weekSummaries = weekSummaries,
        dateFilter = dateFilter
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())
```

- [ ] **Step 6: Remove the now-unused `DateUtils` import**

The old combine was the only place `DateUtils.weekStart()` / `DateUtils.weekEnd()` were called. Remove this line from the imports:

```kotlin
import dev.panthu.sololife.util.DateUtils
```

- [ ] **Step 7: Verify compile**

```bash
cd /data/projects/SoloLife && ./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/expenses/ExpensesViewModel.kt
git commit -m "feat(expenses): add ExpenseDateFilter drill-down to ExpensesViewModel"
```

---

## Chunk 4: Expenses Screen UI

**Files:**
- Modify: `app/src/main/java/dev/panthu/sololife/ui/expenses/ExpensesScreen.kt`

---

- [ ] **Step 1: Add new imports**

Add to `ExpensesScreen.kt` imports (do NOT add `androidx.compose.animation.SideEffect` — it does not exist there; the correct one is from `runtime`):

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.SideEffect
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
```

- [ ] **Step 2: Delete the `WeekSummaryCard` composable and its call site**

Remove the entire `WeekSummaryCard` private composable (lines 132–168 in the original file) and its call site `WeekSummaryCard(weekTotal = state.weekTotal)` in `ExpensesScreen`.

- [ ] **Step 3: Add the `BreadcrumbStrip` composable (final, correct version)**

Add this complete composable before `DayHeader`. Write it once — do not split across multiple steps:

```kotlin
@Composable
private fun BreadcrumbStrip(
    dateFilter: ExpenseDateFilter,
    displayTotal: Double,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    onClearToNone: () -> Unit,
    onClearToMonth: (ExpenseDateFilter.Month) -> Unit
) {
    when (dateFilter) {
        is ExpenseDateFilter.None -> {
            val currentYear = LocalDate.now().year
            val currentMonth = LocalDate.now().monthValue
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(12) { idx ->
                    val monthNum = idx + 1
                    val monthName = Month.of(monthNum)
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    FilterChip(
                        selected = monthNum == currentMonth,
                        onClick = { onMonthSelected(currentYear, monthNum) },
                        label = { Text(monthName) }
                    )
                }
            }
        }
        is ExpenseDateFilter.Month -> {
            val monthName = Month.of(dateFilter.month)
                .getDisplayName(TextStyle.FULL, Locale.getDefault())
            val totalFormatted = "฿${String.format("%,.0f", displayTotal)}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = onClearToNone,
                    label = { Text("← All") }
                )
                FilterChip(
                    selected = true,
                    onClick = onClearToNone,   // tapping active Month chip also clears to None
                    label = { Text("$monthName · $totalFormatted") },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
        is ExpenseDateFilter.Week -> {
            val parentMonth = ExpenseDateFilter.Month(dateFilter.year, dateFilter.month)
            val monthName = Month.of(dateFilter.month)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val monthStart = LocalDate.of(dateFilter.year, dateFilter.month, 1)
            val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
            val displayStart = maxOf(dateFilter.start, monthStart)
            val displayEnd = minOf(dateFilter.end, monthEnd)
            val fmt = DateTimeFormatter.ofPattern("MMM d")
            val rangeLabel = "${displayStart.format(fmt)}–${displayEnd.format(DateTimeFormatter.ofPattern("d"))}"
            val totalFormatted = "฿${String.format("%,.0f", displayTotal)}"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = false,
                    onClick = { onClearToMonth(parentMonth) },
                    label = { Text("← $monthName ${dateFilter.year}") }
                )
                FilterChip(
                    selected = true,
                    onClick = { onClearToMonth(parentMonth) },  // tapping active Week chip → back to Month
                    label = { Text("Wk ${dateFilter.weekIndex + 1}: $rangeLabel · $totalFormatted") },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 6: Add the `WeekCard` composable**

Add this before `DayHeader`:

```kotlin
@Composable
private fun WeekCard(
    summary: WeekSummary,
    onClick: () -> Unit
) {
    val week = summary.filter
    // Clamp display dates to month boundaries
    val monthStart = LocalDate.of(week.year, week.month, 1)
    val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
    val displayStart = maxOf(week.start, monthStart)
    val displayEnd = minOf(week.end, monthEnd)
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    val totalFormatted = "฿${String.format("%,.0f", summary.total)}"
    val maxDaily = summary.dailyTotals.maxOrNull() ?: 0.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Wk ${week.weekIndex + 1}  ·  ${displayStart.format(fmt)}–${displayEnd.format(DateTimeFormatter.ofPattern("d"))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    totalFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Proportional daily bar (Mon–Sun)
            Row(
                modifier = Modifier
                    .height(24.dp)
                    .width(56.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                summary.dailyTotals.forEach { dayTotal ->
                    val fraction = if (maxDaily > 0.0) (dayTotal / maxDaily).toFloat().coerceIn(0f, 1f) else 0f
                    val barHeight = if (fraction == 0f) 1.dp else (fraction * 24).dp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 7: Wire `BreadcrumbStrip` and `AnimatedContent` into `ExpensesScreen`**

Replace the existing `Column` content in `ExpensesScreen` (from the `WeekSummaryCard` call down through the `CategoryBreakdownBar` and lazy list) with:

```kotlin
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Breadcrumb / month scroller
            BreadcrumbStrip(
                dateFilter = state.dateFilter,
                displayTotal = state.displayTotal,
                onMonthSelected = { year, month ->
                    vm.setDateFilter(ExpenseDateFilter.Month(year, month))
                },
                onClearToNone = { vm.clearDateFilter() },
                onClearToMonth = { vm.setDateFilter(it) }
            )

            // Category breakdown bar (always shows full-dataset proportions)
            if (state.allExpenses.isNotEmpty()) {
                CategoryBreakdownBar(
                    allExpenses = state.allExpenses,
                    selectedCategory = state.filterCategory,
                    onCategorySelected = { vm.setFilterCategory(it) }
                )
                Spacer(Modifier.height(8.dp))
            }

            // Animated drill-down content
            val prevDepthRef = remember { intArrayOf(0) }
            val currentDepth = when (state.dateFilter) {
                is ExpenseDateFilter.None -> 0
                is ExpenseDateFilter.Month -> 1
                is ExpenseDateFilter.Week -> 2
            }
            val goingDeeper = currentDepth > prevDepthRef[0]
            SideEffect { prevDepthRef[0] = currentDepth }

            AnimatedContent(
                targetState = state.dateFilter,
                transitionSpec = {
                    slideInHorizontally { if (goingDeeper) it else -it } togetherWith
                    slideOutHorizontally { if (goingDeeper) -it else it }
                },
                modifier = Modifier.weight(1f)
            ) { filter ->
                when {
                    !state.isLoaded -> {
                        Column { repeat(5) { ShimmerExpenseCard() } }
                    }
                    filter is ExpenseDateFilter.Month -> {
                        // Level 1: always show week cards (weeksForMonth always returns 4-6 entries)
                        // Week cards with total=0.0 are valid — user can tap to see the empty list
                        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                            items(state.weekSummaries) { summary ->
                                WeekCard(
                                    summary = summary,
                                    onClick = { vm.setDateFilter(summary.filter) }
                                )
                            }
                        }
                    }
                    state.expenses.isEmpty() -> {
                        ExpensesEmptyState()
                    }
                    else -> {
                        val grouped = remember(state.expenses) { state.expenses.groupByDay() }
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            grouped.forEach { (dayKey, dayExpenses) ->
                                item(key = "day_$dayKey") {
                                    DayHeader(millis = dayKey, expenses = dayExpenses)
                                }
                                items(dayExpenses, key = { it.id }) { expense ->
                                    SwipeActionsContainer(
                                        item = expense,
                                        onDelete = { vm.delete(expense) },
                                        onEdit = { showSheet = false; vm.setEditingExpense(it) }
                                    ) {
                                        ExpenseRow(expense = expense)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
```

- [ ] **Step 8: Verify compile**

```bash
cd /data/projects/SoloLife && ./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`. Fix any import or type errors before proceeding.

- [ ] **Step 9: Full debug build**

```bash
cd /data/projects/SoloLife && ./gradlew :app:assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. APK is at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/dev/panthu/sololife/ui/expenses/ExpensesScreen.kt
git commit -m "feat(expenses): add breadcrumb drill-down and WeekCard with animated transitions"
```

---

## Final Verification Checklist

- [ ] `./gradlew :app:assembleDebug` passes with no errors
- [ ] Diary: switching to CALENDAR view shows month grid; tapping a day with an entry highlights it and shows the filter chip in both CALENDAR and LIST views
- [ ] Diary: tapping the same date again clears the filter; tapping ✕ on chip also clears it
- [ ] Diary: LIST view with active filter only shows entries for the selected day; empty state shown if none
- [ ] Expenses: breadcrumb shows Jan–Dec month chips at top level; tapping a month slides to week cards
- [ ] Expenses: week cards show week label, total, and 7-bar proportional chart; tapping a card slides to expense list
- [ ] Expenses: back navigation (← chips) slides in the correct direction
- [ ] Expenses: category filter composes with date filter at all levels
- [ ] Expenses: empty state shown (with breadcrumb still visible) when no results

---

## Commit Summary

| Commit | Files |
|--------|-------|
| `feat(diary): add selectedDate filter to DiaryViewModel` | `DiaryViewModel.kt` |
| `feat(diary): add date filter chip and tappable calendar` | `DiaryListScreen.kt` |
| `feat(expenses): add ExpenseDateFilter drill-down to ExpensesViewModel` | `ExpensesViewModel.kt` |
| `feat(expenses): add breadcrumb drill-down and WeekCard with animated transitions` | `ExpensesScreen.kt` |
