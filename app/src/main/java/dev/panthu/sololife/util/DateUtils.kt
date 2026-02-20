package dev.panthu.sololife.util

import dev.panthu.sololife.data.db.DailyTotal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    // DateTimeFormatter is immutable and thread-safe
    private val fmtFull    = DateTimeFormatter.ofPattern("EEEE, MMMM d",   Locale.getDefault())
    private val fmtMonth   = DateTimeFormatter.ofPattern("MMMM yyyy",       Locale.getDefault())
    private val fmtShort   = DateTimeFormatter.ofPattern("MMM d",           Locale.getDefault())
    private val fmtDay     = DateTimeFormatter.ofPattern("d",               Locale.getDefault())
    private val fmtDayName = DateTimeFormatter.ofPattern("EEE",             Locale.getDefault())

    private fun Long.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

    fun formatFull(millis: Long): String    = fmtFull.format(millis.toLocalDateTime())
    fun formatMonth(millis: Long): String   = fmtMonth.format(millis.toLocalDateTime())
    fun formatShort(millis: Long): String   = fmtShort.format(millis.toLocalDateTime())
    fun formatDay(millis: Long): String     = fmtDay.format(millis.toLocalDateTime())
    fun formatDayName(millis: Long): String = fmtDayName.format(millis.toLocalDateTime()).uppercase()

    fun todayStart(): Long {
        val today = LocalDate.now(ZoneId.systemDefault())
        return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun todayEnd(): Long = todayStart() + 86_399_999L

    fun weekStart(): Long {
        val today = LocalDate.now(ZoneId.systemDefault())
        // ISO week starts Monday; use DayOfWeek.MONDAY for consistency
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        return monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun weekEnd(): Long = weekStart() + 7 * 86_400_000L - 1L

    fun isSameDay(a: Long, b: Long): Boolean {
        val za = ZoneId.systemDefault()
        val da = Instant.ofEpochMilli(a).atZone(za).toLocalDate()
        val db = Instant.ofEpochMilli(b).atZone(za).toLocalDate()
        return da == db
    }

    fun greetingPrefix(): String {
        val h = LocalTime.now().hour
        return when { h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
    }

    fun dayStart(millis: Long): Long {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun formatRelative(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date > today.minusDays(7) -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            else -> fmtShort.format(date)
        }
    }

    fun currentStreak(dates: List<Long>): Int {
        if (dates.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val entryDays = dates.map {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
        }.toSet()
        var count = 0
        var day = LocalDate.now(zone)
        while (entryDays.contains(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun fillWeekDailyTotals(weekStart: Long, dbRows: List<DailyTotal>): List<DailyTotal> {
        val rowMap = dbRows.associateBy { it.dayStart }
        return (0 until 7).map { i ->
            val dayStartMs = weekStart + i * 86_400_000L
            rowMap[dayStartMs] ?: DailyTotal(dayStartMs, 0.0)
        }
    }
}
