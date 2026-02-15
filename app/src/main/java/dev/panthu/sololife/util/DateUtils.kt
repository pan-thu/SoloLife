package dev.panthu.sololife.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val displayFull   = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val displayMonth  = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val displayShort  = SimpleDateFormat("MMM d", Locale.getDefault())
    private val displayDay    = SimpleDateFormat("d", Locale.getDefault())
    private val displayDayName= SimpleDateFormat("EEE", Locale.getDefault())

    fun formatFull(millis: Long): String   = displayFull.format(Date(millis))
    fun formatMonth(millis: Long): String  = displayMonth.format(Date(millis))
    fun formatShort(millis: Long): String  = displayShort.format(Date(millis))
    fun formatDay(millis: Long): String    = displayDay.format(Date(millis))
    fun formatDayName(millis: Long): String= displayDayName.format(Date(millis)).uppercase()

    fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun todayEnd(): Long = todayStart() + 86_399_999L

    fun weekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun weekEnd(): Long = weekStart() + 7 * 86_400_000L - 1L

    fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().also { it.timeInMillis = a }
        val cb = Calendar.getInstance().also { it.timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
               ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }
}
