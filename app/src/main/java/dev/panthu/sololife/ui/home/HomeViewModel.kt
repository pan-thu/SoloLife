package dev.panthu.sololife.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DailyTotal
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseCategory
import dev.panthu.sololife.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val weekTotal: Double = 0.0,
    val monthTotal: Double = 0.0,
    val lastWeekTotal: Double = 0.0,
    val lastMonthTotal: Double = 0.0,
    val latestDiaryEntry: DiaryEntry? = null,
    val recentExpenses: List<Expense> = emptyList(),
    val weekDailyTotals: List<DailyTotal> = emptyList(),
    val diaryStreak: Int = 0,
    val diaryWeekDays: List<Boolean> = List(7) { false }
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
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val last = today.withDayOfMonth(today.lengthOfMonth())
        return last.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
    }

    fun addExpense(amount: Double, category: ExpenseCategory, description: String, date: Long) {
        viewModelScope.launch {
            expenseRepo.add(Expense(date = date, amount = amount, category = category.name, description = description))
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            expenseRepo.todayTotal(DateUtils.todayStart(), DateUtils.todayEnd()),
            expenseRepo.weekTotal(DateUtils.weekStart(), DateUtils.weekEnd()),
            expenseRepo.weekTotal(monthStart(), monthEnd()),
            diaryRepo.getAll(),
            expenseRepo.getAll().map { it.take(5) }
        ) { today, week, month, allEntries, recent ->
            val zone = ZoneId.systemDefault()
            val weekStartDate = LocalDate.now(zone).with(DayOfWeek.MONDAY)
            val weekDays = (0 until 7).map { i ->
                val day = weekStartDate.plusDays(i.toLong())
                allEntries.any { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() == day }
            }
            HomeUiState(
                todayTotal = today,
                weekTotal = week,
                monthTotal = month,
                latestDiaryEntry = allEntries.firstOrNull(),
                recentExpenses = recent,
                diaryStreak = DateUtils.currentStreak(allEntries.map { it.date }),
                diaryWeekDays = weekDays
            )
        },
        expenseRepo.dailyTotals(DateUtils.weekStart(), DateUtils.weekEnd())
            .map { DateUtils.fillWeekDailyTotals(DateUtils.weekStart(), it) },
        expenseRepo.weekTotal(DateUtils.lastWeekStart(), DateUtils.lastWeekEnd()),
        expenseRepo.weekTotal(DateUtils.lastMonthStart(), DateUtils.lastMonthEnd())
    ) { state, sparkline, lastWeek, lastMonth ->
        state.copy(
            weekDailyTotals = sparkline,
            lastWeekTotal = lastWeek,
            lastMonthTotal = lastMonth
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
