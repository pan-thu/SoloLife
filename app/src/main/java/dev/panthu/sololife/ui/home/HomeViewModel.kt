package dev.panthu.sololife.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.util.DateUtils
import kotlinx.coroutines.flow.*

data class HomeUiState(
    val todayTotal: Double = 0.0,
    val weekTotal: Double = 0.0,
    val latestDiaryEntry: DiaryEntry? = null
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val container   = app as SoloLifeApp
    private val diaryRepo   = container.diaryRepository
    private val expenseRepo = container.expenseRepository

    val uiState: StateFlow<HomeUiState> = combine(
        expenseRepo.todayTotal(DateUtils.todayStart(), DateUtils.todayEnd()),
        expenseRepo.weekTotal(DateUtils.weekStart(), DateUtils.weekEnd()),
        diaryRepo.getAll().map { it.firstOrNull() }
    ) { today: Double, week: Double, latest: DiaryEntry? ->
        HomeUiState(todayTotal = today, weekTotal = week, latestDiaryEntry = latest)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
