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
    val weekTotal: Double = 0.0
)

class ExpensesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SoloLifeApp).expenseRepository

    val uiState: StateFlow<ExpensesUiState> = combine(
        repo.getAll(),
        repo.weekTotal(DateUtils.weekStart(), DateUtils.weekEnd())
    ) { expenses, week ->
        ExpensesUiState(expenses = expenses, weekTotal = week)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())

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

    fun delete(expense: Expense) {
        viewModelScope.launch { repo.delete(expense) }
    }
}
