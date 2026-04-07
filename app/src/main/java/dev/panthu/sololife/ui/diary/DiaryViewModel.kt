package dev.panthu.sololife.ui.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.model.Block
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.encodeUris
import dev.panthu.sololife.util.parseUris
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DiaryListUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val query: String = "",
    val isLoaded: Boolean = false,
    val calendarEntryDates: Set<Long> = emptySet(),
    val currentStreak: Int = 0,
    val selectedDate: java.time.LocalDate? = null
)

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SoloLifeApp).diaryRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedDate = MutableStateFlow<java.time.LocalDate?>(null)

    fun setSelectedDate(date: java.time.LocalDate) {
        _selectedDate.value = date
    }

    fun clearDateFilter() {
        _selectedDate.value = null
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

    private val _allEntries = repo.getAll()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DiaryListUiState> = combine(
        _query.flatMapLatest { q ->
            if (q.isBlank()) _allEntries else repo.search(q)
        },
        run {
            repo.getEntryDatesInRange(currentMonthStart(), currentMonthEnd())
        },
        _allEntries,
        _selectedDate
    ) { filteredEntries, monthDates, allEntries, selectedDate ->
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
            calendarEntryDates = entryDaySet,
            currentStreak = streak,
            selectedDate = selectedDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryListUiState())

    fun setQuery(q: String) { _query.value = q }

    fun delete(entry: DiaryEntry) {
        viewModelScope.launch { repo.softDelete(entry) }
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

    /** Load an entry and return (title, blocks). Migrates legacy entries transparently. */
    suspend fun getEntryAsBlocks(id: Long): Pair<String, List<Block>>? {
        val entry = repo.getById(id) ?: return null
        if (entry.blocksJson.isNotBlank()) {
            runCatching { Json.decodeFromString<List<Block>>(entry.blocksJson) }
                .onSuccess { return entry.title to it }
            // fall through to legacy migration on parse failure
        }
        // Legacy migration: convert HTML content + imageUris → blocks
        val textBlock = Block.Text(
            id = java.util.UUID.randomUUID().toString(),
            html = entry.content
        )
        val imageBlock = if (entry.imageUris.isNotBlank()) {
            Block.Image(
                id = java.util.UUID.randomUUID().toString(),
                paths = parseUris(entry.imageUris)
            )
        } else null
        val blocks = listOfNotNull(textBlock, imageBlock)
        // Persist immediately so migration only runs once
        saveBlocks(id, entry.title, blocks, entry.date)
        return entry.title to blocks
    }

    /** Serialize blocks, keep content + imageUris in sync for search, persist. */
    suspend fun saveBlocks(
        id: Long?,
        title: String,
        blocks: List<Block>,
        date: Long
    ) {
        val blocksJson = Json.encodeToString(blocks)
        // Derive content: concat all Text block HTML for search DAO compatibility
        val content = blocks.filterIsInstance<Block.Text>()
            .joinToString("\n") { it.html }
        // Derive imageUris: collect all Image block paths for backward compat
        val imageUris = encodeUris(
            blocks.filterIsInstance<Block.Image>().flatMap { it.paths }
        )
        if (id == null) {
            repo.save(
                DiaryEntry(
                    date = date,
                    title = title,
                    content = content,
                    imageUris = imageUris,
                    blocksJson = blocksJson
                )
            )
        } else {
            val existing = repo.getById(id) ?: return
            repo.update(
                existing.copy(
                    title = title,
                    content = content,
                    imageUris = imageUris,
                    blocksJson = blocksJson,
                    date = date,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
