package dev.panthu.sololife

import android.app.Application
import dev.panthu.sololife.data.db.SoloLifeDatabase
import dev.panthu.sololife.data.repository.DiaryRepository
import dev.panthu.sololife.data.repository.ExpenseRepository

class SoloLifeApp : Application() {

    val database by lazy { SoloLifeDatabase.getInstance(this) }
    val diaryRepository by lazy { DiaryRepository(database.diaryDao()) }
    val expenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
}
