package dev.panthu.sololife.ui.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseCategory
import dev.panthu.sololife.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val allExpenses: List<Expense> = emptyList(),
    val weekTotal: Double = 0.0,
    val filterCategory: ExpenseCategory? = null,
    val isLoaded: Boolean = false,
    val editingExpense: Expense? = null
)

class ExpensesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SoloLifeApp).expenseRepository

    private val _filterCategory = MutableStateFlow<ExpenseCategory?>(null)
    private val _editingExpense = MutableStateFlow<Expense?>(null)

    fun setFilterCategory(cat: ExpenseCategory?) {
        _filterCategory.value = if (_filterCategory.value == cat) null else cat
    }

    fun setEditingExpense(expense: Expense?) {
        _editingExpense.value = expense
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
        viewModelScope.launch { repo.delete(expense) }
    }

    val uiState: StateFlow<ExpensesUiState> = combine(
        repo.getAll(),
        repo.weekTotal(DateUtils.weekStart(), DateUtils.weekEnd()),
        _filterCategory,
        _editingExpense
    ) { allExpenses, week, filterCat, editing ->
        val filtered = if (filterCat == null) allExpenses
                       else allExpenses.filter { it.category == filterCat.name }
        ExpensesUiState(
            expenses = filtered,
            allExpenses = allExpenses,
            weekTotal = week,
            filterCategory = filterCat,
            isLoaded = true,
            editingExpense = editing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())
}
