package dev.panthu.sololife.ui.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.ui.components.SwipeActionsContainer
import dev.panthu.sololife.util.hapticTick
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RecycleBinUiState(
    val trashedDiary: List<DiaryEntry> = emptyList(),
    val trashedExpenses: List<Expense> = emptyList(),
    val isLoaded: Boolean = false
)

class RecycleBinViewModel(app: Application) : AndroidViewModel(app) {
    private val soloApp     = app as SoloLifeApp
    private val diaryRepo   = soloApp.diaryRepository
    private val expenseRepo = soloApp.expenseRepository

    val uiState: StateFlow<RecycleBinUiState> = combine(
        diaryRepo.getTrashed(),
        expenseRepo.getTrashed()
    ) { diary: List<DiaryEntry>, expenses: List<Expense> ->
        RecycleBinUiState(trashedDiary = diary, trashedExpenses = expenses, isLoaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecycleBinUiState())

    fun restoreDiary(entry: DiaryEntry) {
        viewModelScope.launch { diaryRepo.restore(entry) }
    }

    fun permanentDeleteDiary(entry: DiaryEntry) {
        viewModelScope.launch { diaryRepo.permanentDelete(entry) }
    }

    fun restoreExpense(expense: Expense) {
        viewModelScope.launch { expenseRepo.restore(expense) }
    }

    fun permanentDeleteExpense(expense: Expense) {
        viewModelScope.launch { expenseRepo.permanentDelete(expense) }
    }

    fun purgeAll() {
        viewModelScope.launch {
            diaryRepo.purgeExpiredTrash()
            expenseRepo.purgeExpiredTrash()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    vm: RecycleBinViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val view = LocalView.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(Unit) { vm.purgeAll() }

    val isEmpty = state.isLoaded && state.trashedDiary.isEmpty() && state.trashedExpenses.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Recycle Bin") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (!state.isLoaded) return@Scaffold
        if (isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Recycle bin is empty", style = MaterialTheme.typography.bodyLarge, color = onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Deleted items are kept for 30 days",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (state.trashedDiary.isNotEmpty()) {
                item {
                    Text(
                        "Journal Entries",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
                items(state.trashedDiary, key = { "diary-${it.id}" }) { entry ->
                    SwipeActionsContainer(
                        item = entry,
                        onDelete = { view.hapticTick(); vm.permanentDeleteDiary(entry) },
                        onEdit = { vm.restoreDiary(entry) }
                    ) {
                        TrashRow(
                            title = entry.title.ifBlank { "Untitled" },
                            subtitle = dateFormat.format(Date(entry.date)),
                            deletedAt = entry.deletedAt ?: 0L
                        )
                    }
                }
            }

            if (state.trashedExpenses.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Expenses",
                        style = MaterialTheme.typography.labelMedium,
                        color = onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
                items(state.trashedExpenses, key = { "expense-${it.id}" }) { expense ->
                    SwipeActionsContainer(
                        item = expense,
                        onDelete = { view.hapticTick(); vm.permanentDeleteExpense(expense) },
                        onEdit = { vm.restoreExpense(expense) }
                    ) {
                        TrashRow(
                            title = "฿${String.format("%,.0f", expense.amount)} · ${expense.category}",
                            subtitle = expense.description.ifBlank { dateFormat.format(Date(expense.date)) },
                            deletedAt = expense.deletedAt ?: 0L
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Swipe right to restore · Swipe left to permanently delete\nItems are auto-deleted after 30 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun TrashRow(
    title: String,
    subtitle: String,
    deletedAt: Long
) {
    val daysLeft = remember(deletedAt) {
        val remaining = 30 - ((System.currentTimeMillis() - deletedAt) / (24 * 60 * 60 * 1000))
        remaining.coerceAtLeast(0)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${daysLeft}d left",
            style = MaterialTheme.typography.labelSmall,
            color = if (daysLeft <= 3) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
