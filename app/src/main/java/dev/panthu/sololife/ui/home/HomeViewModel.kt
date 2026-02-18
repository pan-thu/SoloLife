package dev.panthu.sololife.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseCategory
import dev.panthu.sololife.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val weekTotal: Double = 0.0,
    val monthTotal: Double = 0.0,
    val latestDiaryEntry: DiaryEntry? = null,
    val recentExpenses: List<Expense> = emptyList()
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val container   = app as SoloLifeApp
    private val diaryRepo   = container.diaryRepository
    private val expenseRepo = container.expenseRepository

    private fun monthStart(): Long {
        val first = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(1)
        return first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun monthEnd(): Long {
        val last = LocalDate.now(ZoneId.systemDefault()).withDayOfMonth(
            LocalDate.now().lengthOfMonth()
        )
        return last.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 86_399_999L
    }

    fun addExpense(amount: Double, category: ExpenseCategory, description: String, date: Long) {
        viewModelScope.launch {
            expenseRepo.add(Expense(date = date, amount = amount, category = category.name, description = description))
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        expenseRepo.todayTotal(DateUtils.todayStart(), DateUtils.todayEnd()),
        expenseRepo.weekTotal(DateUtils.weekStart(), DateUtils.weekEnd()),
        expenseRepo.weekTotal(monthStart(), monthEnd()),
        diaryRepo.getAll().map { it.firstOrNull() },
        expenseRepo.getAll().map { it.take(5) }
    ) { today: Double, week: Double, month: Double, latest: DiaryEntry?, recent: List<Expense> ->
        HomeUiState(
            todayTotal = today,
            weekTotal = week,
            monthTotal = month,
            latestDiaryEntry = latest,
            recentExpenses = recent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
