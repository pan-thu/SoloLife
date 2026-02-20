package dev.panthu.sololife.data.prefs

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("sololife_prefs", Context.MODE_PRIVATE)

    var diaryNotificationEnabled: Boolean
        get() = prefs.getBoolean("diary_notification_enabled", false)
        set(value) { prefs.edit().putBoolean("diary_notification_enabled", value).apply() }
}
