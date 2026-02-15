package dev.panthu.sololife.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DiaryEntry::class, Expense::class],
    version = 1,
    exportSchema = false
)
abstract class SoloLifeDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: SoloLifeDatabase? = null

        fun getInstance(context: Context): SoloLifeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoloLifeDatabase::class.java,
                    "sololife.db"
                ).build().also { INSTANCE = it }
            }
    }
}
