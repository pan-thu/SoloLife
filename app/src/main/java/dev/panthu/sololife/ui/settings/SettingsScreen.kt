package dev.panthu.sololife.ui.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.SoloLifeApp
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.util.DataTransfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class SettingsExportData(
    val diary: List<DiaryEntry> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val container   = app as SoloLifeApp
    private val diaryRepo   = container.diaryRepository
    private val expenseRepo = container.expenseRepository

    val data = combine(
        diaryRepo.getAll(),
        expenseRepo.getAll()
    ) { diary: List<DiaryEntry>, expenses: List<Expense> ->
        SettingsExportData(diary, expenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsExportData())

    fun export(context: Context, uri: Uri) {
        viewModelScope.launch {
            val d = data.value
            val result = withContext(Dispatchers.IO) {
                DataTransfer.export(context, uri, d.diary, d.expenses)
            }
            result.fold(
                onSuccess = { Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show() },
                onFailure = { Toast.makeText(context, "Export failed: ${it.message ?: "unknown error"}", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    fun import(context: Context, uri: Uri, replace: Boolean) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { DataTransfer.import(context, uri) }
            result.fold(
                onSuccess = { backup ->
                    if (replace) {
                        diaryRepo.replaceAll(backup.diary.map { it.copy(id = 0) })
                        expenseRepo.replaceAll(backup.expenses.map { it.copy(id = 0) })
                    } else {
                        diaryRepo.mergeAll(backup.diary.map { it.copy(id = 0) })
                        expenseRepo.mergeAll(backup.expenses.map { it.copy(id = 0) })
                    }
                    Toast.makeText(
                        context,
                        "Imported ${backup.diary.size} diary entries & ${backup.expenses.size} expenses",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = {
                    Toast.makeText(context, "Import failed: ${it.message ?: "invalid file"}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.export(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { showImportDialog = it } }

    val exportData by vm.data.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Data summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${exportData.diary.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Diary Entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${exportData.expenses.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            "Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Backup & Restore",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            SettingsActionCard(
                icon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                title = "Export Backup",
                subtitle = "Save all diary entries and expenses as JSON",
                onClick = { exportLauncher.launch("sololife_backup.json") }
            )

            SettingsActionCard(
                icon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                title = "Import Backup",
                subtitle = "Restore from a previously exported JSON file",
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
            )

            Spacer(Modifier.weight(1f))
            Text(
                "SoloLife v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // Import mode dialog
    showImportDialog?.let { uri ->
        AlertDialog(
            onDismissRequest = { showImportDialog = null },
            title = { Text("Import Mode") },
            text = { Text("Replace all existing data, or merge (add alongside existing)?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.import(context, uri, replace = true)
                    showImportDialog = null
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.import(context, uri, replace = false)
                    showImportDialog = null
                }) { Text("Merge") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsActionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                icon()
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
