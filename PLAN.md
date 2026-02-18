# SoloLife — 17-Feature Implementation Plan

## Context
The app is functional but visually dated. This plan adds 17 features including a complete UI/UX overhaul to a premium-quality, innovative design. The full UI rework (Feature 17) is the highest priority and is woven throughout every phase rather than treated as a final pass. The design vision is "**Depth & Motion**" — OLED-dark, bold financial numbers, timeline aesthetics, spring-animated interactions, and glassmorphic accents.

---

## Features Overview

| # | Feature | Scope |
|---|---|---|
| 1 | Time-of-day greeting | Hero card shows "Good morning/afternoon/evening" |
| 2 | Animated number counter | Spending totals spring-animate to new value |
| 3 | Relative date labels | "Today", "Yesterday", "Monday" instead of raw dates |
| 4 | 7-day sparkline | Canvas bars on hero Week page showing daily spend trend |
| 5 | Diary calendar heatmap | Toggle list ↔ monthly grid; dots on days with entries |
| 6 | Swipeable hero card | HorizontalPager: Today / This Week / This Month |
| 7 | Haptic feedback | tick/confirm/reject on add, delete, swipe, nav taps |
| 8 | Illustrated empty states | Canvas-drawn open book & wallet, no image assets |
| 9 | Shimmer loading | Skeleton cards while data loads on first open |
| 10 | Bidirectional swipe | Swipe right = edit, swipe left = delete (diary & expenses) |
| 11 | Icon refresh | AutoStories, AccountBalanceWallet, Tune, EditNote, Insights |
| 12 | Rich text editor | Bold / Italic / Underline / Bullet list toolbar in diary |
| 13 | Image attachments | Pick photos from gallery, attach to diary entries |
| 14 | Diary notification | Daily WorkManager job at 22:00 with Settings toggle |
| 15 | Category breakdown bar | Segmented colored bar below week total; tap to filter |
| 16 | Edit expense | Swipe-right opens pre-filled expense form |
| 17 | Full UI/UX rework | Premium redesign across all screens (highest priority) |

---

## New Dependencies

| Library | Coordinate | Purpose |
|---|---|---|
| WorkManager | `androidx.work:work-runtime-ktx:2.10.0` | Daily notification scheduling |
| compose-shimmer | `com.valentinilk.shimmer:compose-shimmer:1.3.1` | Skeleton loading |
| richeditor-compose | `com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc10` | Bold/italic/underline toolbar |
| Coil | `io.coil-kt:coil-compose:2.7.0` | Load attached diary images |

Add version entries to `gradle/libs.versions.toml`; add `implementation(libs.*)` to `app/build.gradle.kts`. `HorizontalPager` is already available in the existing Compose BOM — no extra dependency needed.

---

## Phase 1 — Data Layer

### 1a. New data class
`data/db/DailyTotal.kt` — non-entity projection for sparkline:
```kotlin
data class DailyTotal(val dayStart: Long, val total: Double)
```

### 1b. Room migration v1 → v2 (Feature 13)
- `DiaryEntry.kt`: add `val imageUris: String = ""`
- `SoloLifeDatabase.kt`: bump to version 2, add:
  ```kotlin
  val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE diary_entries ADD COLUMN imageUris TEXT NOT NULL DEFAULT ''")
      }
  }
  ```
  and `.addMigrations(MIGRATION_1_2)` in the builder.

### 1c. New DAO methods
**`DiaryDao.kt`:**
```kotlin
@Query("SELECT date FROM diary_entries WHERE date >= :fromMillis AND date <= :toMillis")
fun getEntryDatesInRange(fromMillis: Long, toMillis: Long): Flow<List<Long>>
```

**`ExpenseDao.kt`:**
```kotlin
@Query("""
  SELECT (date / 86400000) * 86400000 AS dayStart, COALESCE(SUM(amount), 0.0) AS total
  FROM expenses WHERE date >= :fromMillis AND date <= :toMillis
  GROUP BY dayStart ORDER BY dayStart ASC
""")
fun dailyTotals(fromMillis: Long, toMillis: Long): Flow<List<DailyTotal>>

@Update
suspend fun update(expense: Expense)
```

**`DiaryRepository.kt`:** expose `getEntryDatesInRange()`.
**`ExpenseRepository.kt`:** expose `dailyTotals()` and `update()`.

### 1d. SharedPreferences for notification toggle
New file `data/prefs/AppPreferences.kt`:
```kotlin
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sololife_prefs", Context.MODE_PRIVATE)
    var diaryNotificationEnabled: Boolean
        get() = prefs.getBoolean("diary_notification_enabled", false)
        set(value) { prefs.edit().putBoolean("diary_notification_enabled", value).apply() }
}
```
Add `val appPreferences by lazy { AppPreferences(this) }` to `SoloLifeApp.kt`.

---

## Phase 2 — Utilities

### 2a. `util/DateUtils.kt` additions
```kotlin
fun greetingPrefix(): String {
    val h = LocalTime.now().hour
    return when { h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
}

fun dayStart(millis: Long): Long  // truncate to midnight in local zone

fun formatRelative(millis: Long): String
// "Today" / "Yesterday" / "Monday" (full name, ≤7 days) / "Mar 5" (older)

fun currentStreak(dates: List<Long>): Int
// Count consecutive calendar days backward from today

fun fillWeekDailyTotals(weekStart: Long, dbRows: List<DailyTotal>): List<DailyTotal>
// Ensures exactly 7 entries Mon–Sun, filling missing days with total=0.0
```

### 2b. `util/HapticUtils.kt` (new file, Feature 7)
```kotlin
// Called on View from onClick lambdas via rememberHapticFeedback() = LocalView.current
fun View.hapticTick()    // HapticFeedbackConstants.CLOCK_TICK — category select, nav taps
fun View.hapticConfirm() // HapticFeedbackConstants.CONFIRM (API 30+) — add/save
fun View.hapticReject()  // HapticFeedbackConstants.REJECT  (API 30+) — delete confirm
```

### 2c. `util/ImageStorage.kt` (new file, Feature 13)
```kotlin
suspend fun saveImage(context, uri): String  // copy to filesDir/diary_images/, return abs path
fun deleteImage(path: String)
fun pathToUri(context, path): Uri            // FileProvider URI for Coil
fun parseUris(raw: String): List<String>     // split pipe-separated string
fun encodeUris(paths: List<String>): String  // join with "|"
```
Also add to `AndroidManifest.xml`: `<provider android:name="androidx.core.content.FileProvider" …>` with `res/xml/file_paths.xml` pointing to `files-path diary_images/`.

### 2d. `util/NotificationWorker.kt` (new file, Feature 14)
```kotlin
class NotificationWorker(ctx, params) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        // Build and show notification with PendingIntent to MainActivity
        return Result.success()
    }
}

object DiaryNotificationScheduler {
    fun schedule(context: Context) {
        val delay = millisUntil(hour = 22, minute = 0)
        val req = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS).addTag(WORK_TAG).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, req)
    }
    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
}
```
In `SoloLifeApp.onCreate()`: create notification channel (`NOTIFICATION_CHANNEL_ID = "diary_reminder"`).
In `AndroidManifest.xml`: add `POST_NOTIFICATIONS` and `READ_MEDIA_IMAGES` permissions.

---

## Phase 3 — Theme Rework (Feature 17 — Foundation First)

### 3a. `ui/theme/Color.kt` — OLED palette
```
DarkBackground  #070B0F  (was #0E1014 — true near-OLED)
DarkSurface     #0F1319  (was #1A1D23)
DarkSurfaceVar  #171D25  (was #252830)
DarkOutline     #2C3140  (was #3A3D47)
```
Keep sage green primary `#7DD3A8` and amber tertiary `#F4A261` unchanged.

### 3b. `ui/theme/Type.kt` — Bold editorial hierarchy
```
displayLarge  52sp / ExtraBold (800w) / -2sp tracking  ← hero numbers
displaySmall  28sp / SemiBold                           ← unchanged
labelSmall    10sp / Light (300w)                       ← "whisper" labels
titleMedium   15sp / SemiBold / 0sp tracking            ← editorial headers
```

### 3c. Corner radii & spacing convention
| Element | Radius | Horizontal padding |
|---|---|---|
| Hero card, bottom sheets | **28dp** | 24dp |
| Standard section cards | **20dp** | 24dp |
| List item surfaces | **16dp** | 16dp (internal) |
| FAB | **20dp** | — |
| Bottom nav pill indicator | **50dp** (full pill) | — |

Global horizontal screen padding: **24dp** everywhere (was 20dp).
Section spacers: **28dp** (was 24dp).

---

## Phase 4 — Shared Components

### 4a. `SwipeActionsContainer` (replaces `SwipeToDeleteContainer`, Feature 10)
In `SharedComponents.kt` — new composable with:
- `SwipeToDismissBox` with both directions enabled
- `StartToEnd` = edit (primary color, Edit icon, `confirmValueChange` returns `false` so it snaps back)
- `EndToStart` = delete (error color, Delete icon, calls `onDelete()`)
- Spring `animationSpec` on background color

Keep `SwipeToDeleteContainer` if used elsewhere, or update all call sites.

### 4b. `AnimatedAmountText` (Feature 2)
In `SharedComponents.kt`:
```kotlin
@Composable
fun AnimatedAmountText(amount: Double, style: TextStyle, color: Color) {
    val animated by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "amountCounter"
    )
    Text(text = "$${"%.2f".format(animated)}", style = style, color = color, fontWeight = FontWeight.ExtraBold)
}
```

### 4c. Shimmer cards (Feature 9)
New file `ui/components/ShimmerComponents.kt`:
- `ShimmerDiaryCard()` — uses `Modifier.shimmer()` from compose-shimmer, draws placeholder rectangles matching `DiaryTimelineItem` shape
- `ShimmerExpenseCard()` — matches `ExpenseRow` shape

### 4d. Illustrated empty states (Feature 8)
New file `ui/components/IllustratedEmptyStates.kt`:
- `DiaryEmptyState()` — Canvas-drawn open book (two rounded rectangles, spine line, horizontal content lines, quill curve)
- `ExpensesEmptyState()` — Canvas-drawn wallet (rounded rect body, card slot, coin circle)
- No image assets — pure `Canvas` drawing using `drawRoundRect`, `drawLine`, `drawCircle`

---

## Phase 5 — HomeScreen (Features 1, 2, 4, 6)

### 5a. `HomeUiState` additions
```kotlin
data class HomeUiState(
    // existing fields …
    val weekDailyTotals: List<DailyTotal> = emptyList(),  // Feature 4: sparkline
    // greeting computed in composable from system clock — not in VM
)
```

### 5b. `HomeViewModel` — nested combine
Use a 5-way combine + a second combine with the sparkline flow (nested approach to avoid array overload):
```kotlin
val uiState = combine(
    combine(todayFlow, weekFlow, monthFlow, latestDiaryFlow, recentExpensesFlow) { ... },
    expenseRepo.dailyTotals(weekStart, weekEnd).map { fillWeekDailyTotals(weekStart, it) }
) { state, sparkline -> state.copy(weekDailyTotals = sparkline) }.stateIn(...)
```

### 5c. `HeroCard` → `HeroCardPager` (Features 1, 4, 6, 17)
Replace with `HorizontalPager(pageCount = 3)` wrapped in a `Column`:

**Page 0 — Today:**
- Greeting: `DateUtils.greetingPrefix()` in labelLarge / Light weight (Feature 1)
- Date in titleMedium
- `AnimatedAmountText(todayTotal, displayLarge)` — 52sp ExtraBold (Features 2, 17)
- Full-pill `Button(shape = CircleShape)` for Add — no more FilledTonalButton (Feature 17)

**Page 1 — Week:**
- `AnimatedAmountText(weekTotal, displayLarge)`
- `WeekSparkline(dailyTotals)` — Canvas-drawn 7 bars (Feature 4):
  - Bars proportional to max daily spend; rounded tops; today's bar uses primary color, others primary at 40% alpha; zero-spend days draw faint outline bar

**Page 2 — Month:**
- `AnimatedAmountText(monthTotal, displayLarge)`

**Dot indicator below pager:** animated width pill (20dp selected / 6dp unselected) using `animateDpAsState` with spring.

**`HeroSurface`:** diagonal `Brush.linearGradient(primary 18% → tertiary 10% → transparent)` + 0.5dp border gradient for glass effect. Corner 28dp.

### 5d. Stat cards (Feature 17)
- Add watermark icon (Insights / CalendarMonth) at 5% alpha as `Box` overlay behind content
- Spring entrance animations replacing `tween()` throughout `HomeScreen`
- Increase corner to 20dp on `StatCard`

---

## Phase 6 — DiaryListScreen (Features 3, 5, 8, 9, 10, 17)

### 6a. `DiaryListUiState` additions
```kotlin
data class DiaryListUiState(
    val entries: List<DiaryEntry> = emptyList(),
    val query: String = "",
    val isLoaded: Boolean = false,
    val viewMode: DiaryViewMode = DiaryViewMode.LIST,
    val calendarEntryDates: Set<Long> = emptySet(),  // day-start millis set
    val currentStreak: Int = 0
)
enum class DiaryViewMode { LIST, CALENDAR }
```

### 6b. `DiaryViewModel` additions
- `toggleViewMode()` — flips `_viewMode` MutableStateFlow
- 4-way combine: entries + viewMode + `getEntryDatesInRange(monthStart, monthEnd)` + allEntries for streak
- `save()` gains `imageUris: String = ""` parameter

### 6c. Visual changes — Timeline list (Feature 17)
Replace `DiaryEntryCard` with `DiaryTimelineItem`:
- Left column (40dp wide): 10dp dot (CircleShape, primary color) + 2dp vertical line (outline 20% alpha) running to next item
- Right card: Surface 20dp corners, title + `DateUtils.formatRelative(date)` as whisper label (Feature 3), HTML-stripped 2-line preview
- Month headers: pill `Surface(shape = RoundedCornerShape(50%))` with `formatMonth()` — float left, not full-width (Feature 17)

### 6d. Top bar additions
- Toggle icon: `GridView` ↔ `FormatListBulleted` — calls `vm.toggleViewMode()`
- Streak badge pill: `LocalFireDepartment` icon + count in tertiary color (Feature 5)

### 6e. `DiaryCalendarView` composable (Feature 5)
- `LazyVerticalGrid(columns = Fixed(7))` — current month
- Leading empty cells for `firstOfMonth.dayOfWeek` offset
- Each day: aspect ratio 1:1, today highlighted (primary 20% bg), entry dates show filled 4dp dot below number
- Tapping a day with an entry navigates to that entry via `onDayClick(dayMillis)`

### 6f. Swipe + shimmer + empty state
- `SwipeActionsContainer(onDelete = vm::delete, onEdit = { onOpenEntry(it.id) })`
- Shimmer: show `repeat(5) { ShimmerDiaryCard() }` while `!state.isLoaded`
- Empty: `DiaryEmptyState()` (Feature 8)

---

## Phase 7 — DiaryDetailScreen (Features 12, 13)

### 7a. Rich text editor (Feature 12)
- Replace content `BasicTextField` with `RichTextEditor(state = richTextState)` from richeditor-compose
- `rememberRichTextState()` — load via `richTextState.setHtml(entry.content)` if content starts with `<`, else `richTextState.setText(content)` (backward compatible)
- On save: `val html = richTextState.toHtml()`

### 7b. FormattingToolbar (Feature 12)
Shown as `bottomBar` in Scaffold when `WindowInsets.isImeVisible`:
```
[B] [I] [U] [• List]  — each toggles via richTextState.toggleSpanStyle(SpanStyle(...))
```
`isUnorderedList` / `toggleUnorderedList()` from RichTextState API.

### 7c. Image attachments (Feature 13)
```kotlin
val pickImage = rememberLauncherForActivityResult(GetContent()) { uri ->
    uri?.let { scope.launch { imageUris += ImageStorage.saveImage(context, it) } }
}
```
- Add `AddPhotoAlternate` icon button in top bar actions
- Image strip: `LazyRow` of 80dp × 80dp `AsyncImage` (Coil) tiles with 12dp corners + ×-button overlay
- Remove: `imageUris -= path; ImageStorage.deleteImage(path)`

---

## Phase 8 — ExpensesScreen (Features 3, 9, 10, 15, 16, 17)

### 8a. `ExpensesUiState` + `ExpensesViewModel` additions
```kotlin
data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),    // filtered by filterCategory
    val allExpenses: List<Expense> = emptyList(), // unfiltered (for breakdown bar)
    val weekTotal: Double = 0.0,
    val filterCategory: ExpenseCategory? = null,
    val isLoaded: Boolean = false,
    val editingExpense: Expense? = null
)
```
- `_filterCategory: MutableStateFlow<ExpenseCategory?>` — `setFilterCategory(cat)` toggles
- `updateExpense(expense: Expense)` — calls `repo.update(expense)`
- `setEditingExpense(expense: Expense?)` — updates `_editingExpense`

### 8b. `ExpenseFormSheet.kt` (renamed from `AddExpenseSheet.kt`, Feature 16)
- Signature: `ExpenseFormSheet(expense: Expense? = null, onDismiss, onSave)`
- Pre-fill all fields when `expense != null`; button label "Save Changes" vs "Add Expense"
- Sheet corner: 28dp top corners

### 8c. `CategoryBreakdownBar` (Feature 15)
In `SharedComponents.kt`:
- Horizontal `Row` of `Box` segments, each `weight(fraction)` based on category share
- Filled with `catInfo.color` at full alpha (selected) or 30% alpha (unselected)
- 1dp gaps between segments; fully rounded ends via parent `Modifier.clip(RoundedCornerShape(5.dp))`
- Below: `LazyRow` of `FilterChip` per category with colored border/container when selected
- Placed below `WeekSummaryCard` in `ExpensesScreen`

### 8d. Expense row visual changes (Feature 17)
Replace row layout: `Box` with 3dp category-colored left border + `Surface(padding(start=3.dp))` for content.
Relative day headers using `DateUtils.formatRelative(millis)` (Feature 3).

### 8e. Swipe + shimmer + empty state
- `SwipeActionsContainer(onDelete = vm::delete, onEdit = { vm.setEditingExpense(it) })`
- `ExpenseFormSheet` hosted at screen level; shown when `state.editingExpense != null`
- Shimmer + `ExpensesEmptyState()` gated on `state.isLoaded`

---

## Phase 9 — SettingsScreen (Feature 14)

Add "Reminders" section above Backup:
- `Switch` for "Daily Diary Reminder — 10:00 PM"
- On toggle ON: request `POST_NOTIFICATIONS` permission via `rememberLauncherForActivityResult(RequestPermission())`; if granted → `vm.setNotificationEnabled(context, true)` → `DiaryNotificationScheduler.schedule(context)`
- On toggle OFF: `vm.setNotificationEnabled(context, false)` → `DiaryNotificationScheduler.cancel(context)`

`SettingsViewModel`: add `_notificationEnabled: MutableStateFlow<Boolean>` from `appPreferences.diaryNotificationEnabled`.

---

## Phase 10 — Navigation & Icon Refresh (Features 7, 11, 17)

### Animated bottom nav (Feature 17)
Replace `NavigationBar` + `NavigationBarItem` in `MainActivity.kt` with custom `AnimatedBottomNav`:
- `BoxWithConstraints` to compute item width
- `Box` pill indicator animated via `animateDpAsState(offset, spring(MediumBouncy))` — slides horizontally
- Each item: `clickable(indication = null)` + icon in primary (selected) or onSurfaceVariant color
- Label: `AnimatedVisibility(selected)` fades in below icon only when selected

### Icon replacements (Feature 11)
| Location | Old | New |
|---|---|---|
| Bottom nav: Diary | `MenuBook` | `AutoStories` |
| Bottom nav: Expenses | `ReceiptLong` | `AccountBalanceWallet` |
| Bottom nav: Settings | `Settings` | `Tune` |
| DiaryListScreen FAB | `Add` | `EditNote` |
| Stat card watermarks | — | `Insights` / `CalendarMonth` (5% alpha) |

### Haptic touchpoints (Feature 7)
Apply `rememberHapticFeedback()` (= `LocalView.current`) in:
- `AnimatedBottomNav` nav taps → `hapticTick()`
- Category pill selection in `ExpenseFormSheet` → `hapticTick()`
- Add/Save button in `ExpenseFormSheet` → `hapticConfirm()`
- `SwipeActionsContainer` delete confirm → `hapticReject()`
- `SwipeActionsContainer` edit trigger → `hapticTick()`
- DiaryList / Expense FAB → `hapticConfirm()`

---

## Implementation Order

```
1.  Gradle sync (add 4 deps)
2.  DailyTotal.kt data class
3.  DiaryEntry + SoloLifeDatabase migration (compile-check Room)
4.  New DAO methods + repositories
5.  AppPreferences.kt + SoloLifeApp wiring
6.  HapticUtils.kt, ImageStorage.kt + FileProvider manifest entries
7.  NotificationWorker.kt + channel + manifest permissions
8.  DateUtils additions (greetingPrefix, formatRelative, dayStart, currentStreak, fillWeekDailyTotals)
9.  Color.kt + Type.kt rework  ← visual foundation
10. SharedComponents: AnimatedAmountText, SwipeActionsContainer
11. ShimmerComponents.kt, IllustratedEmptyStates.kt
12. HomeViewModel (nested combine + weekDailyTotals in UiState)
13. HomeScreen — HeroCardPager, WeekSparkline, stat cards, spring animations
14. ExpensesViewModel (filter, edit, isLoaded state)
15. ExpenseFormSheet.kt (renamed + pre-fill support)
16. ExpensesScreen — breakdown bar, SwipeActionsContainer, shimmer, expense row borders, relative dates
17. DiaryViewModel (4-way combine + isLoaded, streak, calDates, viewMode)
18. DiaryListScreen — timeline layout, pill month headers, calendar heatmap, shimmer, SwipeActionsContainer, streak badge
19. DiaryDetailScreen — RichTextEditor, FormattingToolbar, image picker
20. SettingsViewModel + SettingsScreen notification section
21. MainActivity — AnimatedBottomNav + icon replacements + haptics
22. Global spacing/padding audit (20dp → 24dp everywhere at screen level)
23. Build + fix compilation errors
24. git commit: feat: implement 17 features and full UI/UX rework
```

---

## Critical Files

| File | Change |
|---|---|
| `app/build.gradle.kts` + `gradle/libs.versions.toml` | 4 new dependencies |
| `data/db/DiaryEntry.kt` | Add `imageUris` field |
| `data/db/SoloLifeDatabase.kt` | Version 2, MIGRATION_1_2 |
| `data/db/DiaryDao.kt` | `getEntryDatesInRange()` |
| `data/db/ExpenseDao.kt` | `dailyTotals()`, `update()` |
| `data/prefs/AppPreferences.kt` | **New file** |
| `util/HapticUtils.kt` | **New file** |
| `util/ImageStorage.kt` | **New file** |
| `util/NotificationWorker.kt` | **New file** |
| `util/DateUtils.kt` | 5 new functions |
| `ui/theme/Color.kt` | OLED dark palette |
| `ui/theme/Type.kt` | 52sp ExtraBold display, Light whisper labels |
| `ui/components/SharedComponents.kt` | AnimatedAmountText, SwipeActionsContainer |
| `ui/components/ShimmerComponents.kt` | **New file** |
| `ui/components/IllustratedEmptyStates.kt` | **New file** |
| `ui/home/HomeViewModel.kt` | Nested combine, weekDailyTotals |
| `ui/home/HomeScreen.kt` | Full rework — HeroCardPager, sparkline |
| `ui/diary/DiaryViewModel.kt` | 4-way combine, imageUris in save() |
| `ui/diary/DiaryListScreen.kt` | Timeline, calendar, shimmer, toggle |
| `ui/diary/DiaryDetailScreen.kt` | RichTextEditor, toolbar, images |
| `ui/expenses/ExpensesViewModel.kt` | Filter, edit, isLoaded |
| `ui/expenses/ExpenseFormSheet.kt` | Renamed + pre-fill support |
| `ui/expenses/ExpensesScreen.kt` | Breakdown bar, borders, edit flow |
| `ui/settings/SettingsViewModel.kt` | Notification state |
| `ui/settings/SettingsScreen.kt` | Notification toggle + permission |
| `MainActivity.kt` | AnimatedBottomNav, icons, haptics |
| `SoloLifeApp.kt` | Notification channel, appPreferences |
| `AndroidManifest.xml` | Permissions + FileProvider |
| `res/xml/file_paths.xml` | **New file** (FileProvider paths) |

---

## Verification Checklist

1. **Greeting:** Open at different hours → correct morning/afternoon/evening shown
2. **Animated counter:** Add expense → today total springs up with bounce
3. **Relative dates:** Expense from yesterday shows "Yesterday" as day header
4. **Sparkline:** Swipe hero to Week page → 7 bars reflect per-day spending; today bar brighter
5. **Calendar heatmap:** Tap grid icon in Diary → month grid with dots on entry days
6. **Swipeable hero:** Horizontal swipe between Today/Week/Month pages; dot indicator slides
7. **Haptics:** Add expense → confirm vibration; delete → reject vibration; nav taps → tick
8. **Empty states:** Clear all entries → illustrated Canvas open-book or wallet visible
9. **Shimmer:** Fresh install → shimmer cards briefly appear before data loads
10. **Bidirectional swipe:** Swipe right on expense → pre-filled edit sheet opens; swipe left → deleted
11. **Icons:** Diary nav = AutoStories, Expenses = AccountBalanceWallet, Settings = Tune
12. **Rich text:** Bold/italic/underline applied in editor, persists after save/reopen (HTML stored in `content`)
13. **Images:** Pick photo → appears in strip; reopen entry → images still there; remove → file deleted
14. **Notification:** Toggle ON in Settings → permission requested → WorkManager schedules 22:00 daily job; toggle OFF → job cancelled
15. **Breakdown bar:** Add mixed-category expenses → segmented bar appears; tap segment → list filters
16. **Edit expense:** Swipe right on expense → form opens pre-filled → change amount → save → list updates
17. **UI/UX rework:** OLED near-black background; hero numbers at 52sp ExtraBold; diagonal gradient on hero card; timeline left-border in diary; 3dp category-colored left border on expense rows; animated sliding bottom nav pill; stat card watermark icons at 5% opacity
