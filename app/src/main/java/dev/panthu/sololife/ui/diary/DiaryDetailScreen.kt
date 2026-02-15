package dev.panthu.sololife.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.panthu.sololife.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    entryId: Long?,
    onBack: () -> Unit,
    vm: DiaryViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var loaded by remember { mutableStateOf(entryId == null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Load existing entry
    LaunchedEffect(entryId) {
        if (entryId != null) {
            vm.getEntry(entryId)?.let { entry ->
                title = entry.title
                content = entry.content
                date = entry.date
            }
            loaded = true
        }
    }

    val contentFocus = remember { FocusRequester() }

    fun save() {
        if (title.isBlank() && content.isBlank()) { onBack(); return }
        scope.launch {
            vm.save(entryId, title.trim(), content.trim(), date)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = ::save) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Save & go back")
                    }
                },
                title = {
                    // Tappable date — opens date picker
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "Change date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = DateUtils.formatShort(date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::save) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Title field
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = MaterialTheme.typography.headlineLarge.lineHeight
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(Modifier.height(20.dp))

            // Content field
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                "Write your thoughts here…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp)
                    .focusRequester(contentFocus)
            )
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
