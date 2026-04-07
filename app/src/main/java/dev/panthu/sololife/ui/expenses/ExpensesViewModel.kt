package dev.panthu.sololife.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseCategory
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val allExpenses: List<Expense> = emptyList(),
    val displayTotal: Double = 0.0,
    val filterCategory: ExpenseCategory? = null,
    val isLoaded: Boolean = false,
    val editingExpense: Expense? = null,
    val weekSummaries: List<WeekSummary> = emptyList(),
    val dateFilter: ExpenseDateFilter = ExpenseDateFilter.None
)

class ExpensesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SoloLifeApp).expenseRepository

    private val _filterCategory = MutableStateFlow<ExpenseCategory?>(null)
    private val _editingExpense = MutableStateFlow<Expense?>(null)
    private val _dateFilter = MutableStateFlow<ExpenseDateFilter>(ExpenseDateFilter.None)

    fun setFilterCategory(cat: ExpenseCategory?) {
        _filterCategory.value = if (_filterCategory.value == cat) null else cat
    }

    fun setEditingExpense(expense: Expense?) {
        _editingExpense.value = expense
    }

    fun setDateFilter(filter: ExpenseDateFilter) {
        _dateFilter.value = filter
    }

    fun clearDateFilter() {
        _dateFilter.value = ExpenseDateFilter.None
    }

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

    fun addExpense(amount: Double, category: ExpenseCategory, description: String, date: Long) {
        viewModelScope.launch {
            repo.add(
                Expense(
                    date = date,
                    amount = amount,
                    category = category.name,
                    description = description
                )
            )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { repo.update(expense) }
    }

    fun delete(expense: Expense) {
        viewModelScope.launch { repo.softDelete(expense) }
    }

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

        // Step 3: displayTotal — always reflects current filtered result (date + category)
        val displayTotal = filtered.sumOf { it.amount }

        // Step 4: weekSummaries (only when drilling into a month)
        val weekSummaries = if (dateFilter is ExpenseDateFilter.Month) {
            weeksForMonth(dateFilter.year, dateFilter.month).map { week ->
                val weekExpenses = allExpenses.filter {
                    it.date.toLocalDate() in week.start..week.end
                }
                val byDay: Map<LocalDate, List<Expense>> =
                    weekExpenses.groupBy { it.date.toLocalDate() }
                val dailyTotals = List(7) { dayIdx ->
                    val targetDay = week.start.plusDays(dayIdx.toLong())
                    byDay[targetDay]?.sumOf { it.amount } ?: 0.0
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
}
