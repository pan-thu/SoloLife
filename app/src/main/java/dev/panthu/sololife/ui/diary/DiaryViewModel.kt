package dev.panthu.sololife.ui.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class DiaryViewMode { LIST, CALENDAR }

data class DiaryListUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val query: String = "",
    val isLoaded: Boolean = false,
    val viewMode: DiaryViewMode = DiaryViewMode.LIST,
    val calendarEntryDates: Set<Long> = emptySet(),
    val currentStreak: Int = 0
)

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SoloLifeApp).diaryRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _viewMode = MutableStateFlow(DiaryViewMode.LIST)

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == DiaryViewMode.LIST) DiaryViewMode.CALENDAR else DiaryViewMode.LIST
    }

    private fun currentMonthStart(): Long {
        val zone = java.time.ZoneId.systemDefault()
        val first = java.time.LocalDate.now(zone).withDayOfMonth(1)
        return first.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun currentMonthEnd(): Long {
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        val last = today.withDayOfMonth(today.lengthOfMonth())
        return last.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DiaryListUiState> = combine(
        _query.flatMapLatest { q ->
            if (q.isBlank()) repo.getAll() else repo.search(q)
        },
        _viewMode,
        run {
            repo.getEntryDatesInRange(currentMonthStart(), currentMonthEnd())
        },
        repo.getAll()
    ) { filteredEntries, viewMode, monthDates, allEntries ->
        val entryDaySet = monthDates.map { DateUtils.dayStart(it) }.toSet()
        val streak = DateUtils.currentStreak(allEntries.map { it.date })
        DiaryListUiState(
            entries = filteredEntries,
            query = _query.value,
            isLoaded = true,
            viewMode = viewMode,
            calendarEntryDates = entryDaySet,
            currentStreak = streak
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryListUiState())

    fun setQuery(q: String) { _query.value = q }

    fun delete(entry: DiaryEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }

    suspend fun getEntry(id: Long): DiaryEntry? = repo.getById(id)

    suspend fun save(
        id: Long?,
        title: String,
        content: String,
        date: Long,
        imageUris: String = ""
    ) {
        if (id == null) {
            repo.save(DiaryEntry(date = date, title = title, content = content, imageUris = imageUris))
        } else {
            val existing = repo.getById(id) ?: return
            repo.update(existing.copy(
                title = title,
                content = content,
                date = date,
                imageUris = imageUris,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
}
