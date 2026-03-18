package dev.panthu.sololife.ui.expenses

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.ui.components.AmountText
import dev.panthu.sololife.ui.components.CategoryBreakdownBar
import dev.panthu.sololife.ui.components.ExpensesEmptyState
import dev.panthu.sololife.ui.components.ShimmerExpenseCard
import dev.panthu.sololife.ui.components.SwipeActionsContainer
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.hapticConfirm
import dev.panthu.sololife.util.info
import dev.panthu.sololife.util.toExpenseCategory
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

// Depth extension for ExpenseDateFilter navigation
private fun ExpenseDateFilter.depth() = when (this) {
    is ExpenseDateFilter.None -> 0
    is ExpenseDateFilter.Month -> 1
    is ExpenseDateFilter.Week -> 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(vm: ExpensesViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val view = LocalView.current
    val today = remember { LocalDate.now() }
    var selectedYear by remember { mutableStateOf(today.year) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (state.dateFilter is ExpenseDateFilter.None) {
                        IconButton(onClick = { selectedYear-- }) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous year")
                        }
                        Text(
                            text = selectedYear.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        IconButton(
                            onClick = { selectedYear++ },
                            enabled = selectedYear < today.year
                        ) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next year")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { view.hapticConfirm(); vm.setEditingExpense(null); showSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add expense")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                selectedYear = selectedYear,
                onYearChange = { selectedYear = it },
                onMonthSelected = { year, month -> vm.setDateFilter(ExpenseDateFilter.Month(year, month)) },
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
            AnimatedContent(
                targetState = state.dateFilter,
                transitionSpec = {
                    val deeper = targetState.depth() > initialState.depth()
                    slideInHorizontally { if (deeper) it else -it } togetherWith
                    slideOutHorizontally { if (deeper) -it else it }
                },
                modifier = Modifier.weight(1f)
            ) { filter ->
                when {
                    !state.isLoaded -> {
                        Column { repeat(5) { ShimmerExpenseCard() } }
                    }
                    filter is ExpenseDateFilter.Month -> {
                        // Level 1: always show week cards (weeksForMonth always returns 4-6 entries)
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

    // Add expense sheet
    if (showSheet) {
        ExpenseFormSheet(
            expense = null,
            onDismiss = { showSheet = false },
            onSave = { amount, category, description, date ->
                vm.addExpense(amount, category, description, date)
            }
        )
    }

    // Edit expense sheet
    state.editingExpense?.let { editingExpense ->
        ExpenseFormSheet(
            expense = editingExpense,
            onDismiss = { vm.setEditingExpense(null) },
            onSave = { amount, category, description, date ->
                vm.updateExpense(editingExpense.copy(amount = amount, category = category.name, description = description, date = date))
                vm.setEditingExpense(null)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreadcrumbStrip(
    dateFilter: ExpenseDateFilter,
    displayTotal: Double,
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    onClearToNone: () -> Unit,
    onClearToMonth: (ExpenseDateFilter.Month) -> Unit
) {
    val today = remember { LocalDate.now() }
    when (dateFilter) {
        is ExpenseDateFilter.None -> {
            val currentMonth = if (selectedYear == today.year) today.monthValue else -1
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(12) { idx ->
                    val monthNum = idx + 1
                    val monthName = Month.of(monthNum).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    FilterChip(
                        selected = monthNum == currentMonth,
                        onClick = { onMonthSelected(selectedYear, monthNum) },
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
                summary.dailyTotals.take(7).forEach { dayTotal ->
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

@Composable
private fun DayHeader(millis: Long, expenses: List<Expense>) {
    val dayTotal = expenses.sumOf { it.amount }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = DateUtils.formatRelative(millis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        AmountText(amount = dayTotal, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ExpenseRow(expense: Expense) {
    val category = expense.category.toExpenseCategory()
    val info = category.info()

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // 3dp colored left border
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(info.color, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(info.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = info.icon,
                        contentDescription = null,
                        tint = info.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (expense.description.isBlank()) info.label else expense.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    if (expense.description.isNotBlank()) {
                        Text(
                            text = info.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = info.color
                        )
                    }
                }

                AmountText(
                    amount = expense.amount,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun List<Expense>.groupByDay(): Map<Long, List<Expense>> {
    val result = LinkedHashMap<Long, MutableList<Expense>>()
    forEach { expense ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = expense.date
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val key = cal.timeInMillis
        result.getOrPut(key) { mutableListOf() }.add(expense)
    }
    return result
}
