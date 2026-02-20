package dev.panthu.sololife.ui.diary

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import dev.panthu.sololife.util.DateUtils
import dev.panthu.sololife.util.deleteImage
import dev.panthu.sololife.util.encodeUris
import dev.panthu.sololife.util.parseUris
import dev.panthu.sololife.util.pathToUri
import dev.panthu.sololife.util.saveImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    entryId: Long?,
    onBack: () -> Unit,
    vm: DiaryViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    val richTextState = rememberRichTextState()
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var loaded by remember { mutableStateOf(entryId == null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    // Paths staged for deletion — only deleted from disk inside save()
    var pendingDeletes by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load existing entry
    LaunchedEffect(entryId) {
        if (entryId != null) {
            val entry = vm.getEntry(entryId)
            if (entry == null) {
                onBack()
                return@LaunchedEffect
            }
            title = entry.title
            // Backward compat: old entries contain plain text, new ones HTML
            if (entry.content.trimStart().startsWith("<")) {
                richTextState.setHtml(entry.content)
            } else {
                richTextState.setText(entry.content)
            }
            date = entry.date
            imagePaths = parseUris(entry.imageUris)
            loaded = true
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = saveImage(context, it)
                imagePaths = imagePaths + path
            }
        }
    }

    fun save() {
        val content = richTextState.toHtml()
        if (title.isBlank() && richTextState.annotatedString.text.isBlank()) {
            onBack()
            return
        }
        scope.launch {
            vm.save(entryId, title.trim(), content, date, encodeUris(imagePaths))
            // Only delete files after the entry is safely persisted
            pendingDeletes.forEach { deleteImage(it) }
            onBack()
        }
    }

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val isImeVisible = imeBottom > 0

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = ::save) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Save & go back")
                    }
                },
                title = {
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
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(
                            Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Add photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
        bottomBar = {
            AnimatedVisibility(visible = isImeVisible) {
                FormattingToolbar(richTextState)
            }
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
        ) {
            // Image thumbnail strip
            if (imagePaths.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(imagePaths) { path ->
                        ImageThumbnail(
                            path = path,
                            context = context,
                            onDelete = {
                                imagePaths = imagePaths - path
                                pendingDeletes = pendingDeletes + path
                            }
                        )
                    }
                }
            }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Rich text content editor
            RichTextEditor(
                state = richTextState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                ),
                placeholder = {
                    Text(
                        "Write your thoughts here…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
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

@Composable
private fun FormattingToolbar(state: com.mohamedrejeb.richeditor.model.RichTextState) {
    val isBold = state.currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = state.currentSpanStyle.fontStyle == FontStyle.Italic
    val isUnderline = state.currentSpanStyle.textDecoration
        ?.contains(TextDecoration.Underline) == true
    val isBulletList = state.isUnorderedList

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatButton(
                icon = Icons.Rounded.FormatBold,
                active = isBold,
                onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            )
            FormatButton(
                icon = Icons.Rounded.FormatItalic,
                active = isItalic,
                onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            )
            FormatButton(
                icon = Icons.Rounded.FormatUnderlined,
                active = isUnderline,
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
            )
            FormatButton(
                icon = Icons.Rounded.FormatListBulleted,
                active = isBulletList,
                onClick = { state.toggleUnorderedList() }
            )
        }
    }
}

@Composable
private fun FormatButton(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ImageThumbnail(path: String, context: Context, onDelete: () -> Unit) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = pathToUri(context, path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove image",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
