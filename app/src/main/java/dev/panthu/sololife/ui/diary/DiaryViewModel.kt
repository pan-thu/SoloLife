package dev.panthu.sololife.ui.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DiaryEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DiaryListUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val isSearchActive: Boolean = false,
    val query: String = ""
)

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as SoloLifeApp).diaryRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DiaryListUiState> = _query.flatMapLatest { q ->
        if (q.isBlank()) repo.getAll() else repo.search(q)
    }.map { entries ->
        DiaryListUiState(entries = entries, query = _query.value)
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
        date: Long
    ) {
        if (id == null) {
            repo.save(DiaryEntry(date = date, title = title, content = content))
        } else {
            val existing = repo.getById(id) ?: return
            repo.update(
                existing.copy(
                    title = title,
                    content = content,
                    date = date,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
