package dev.panthu.sololife.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(vm: ExpensesViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { view.hapticConfirm(); showSheet = true },
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
            // Week summary header
            WeekSummaryCard(weekTotal = state.weekTotal)

            // Category breakdown bar (shown when there are expenses)
            if (state.allExpenses.isNotEmpty()) {
                CategoryBreakdownBar(
                    allExpenses = state.allExpenses,
                    selectedCategory = state.filterCategory,
                    onCategorySelected = { vm.setFilterCategory(it) }
                )
                Spacer(Modifier.height(8.dp))
            }

            if (!state.isLoaded) {
                repeat(5) { ShimmerExpenseCard() }
            } else if (state.expenses.isEmpty()) {
                ExpensesEmptyState()
            } else {
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
                                onEdit = { vm.setEditingExpense(it) }
                            ) {
                                ExpenseRow(expense = expense)
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

@Composable
private fun WeekSummaryCard(weekTotal: Double) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "This Week",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                AmountText(
                    amount = weekTotal,
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Text(
                text = "${DateUtils.formatShort(DateUtils.weekStart())} – ${DateUtils.formatShort(DateUtils.weekEnd())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
