package dev.panthu.sololife

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import dev.panthu.sololife.data.db.SoloLifeDatabase
import dev.panthu.sololife.data.prefs.AppPreferences
import dev.panthu.sololife.data.repository.DiaryRepository
import dev.panthu.sololife.data.repository.ExpenseRepository

class SoloLifeApp : Application() {

    val database by lazy { SoloLifeDatabase.getInstance(this) }
    val diaryRepository by lazy { DiaryRepository(database.diaryDao()) }
    val expenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
    val appPreferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Diary Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "diary_reminder"
    }
}
