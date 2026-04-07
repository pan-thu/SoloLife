package dev.panthu.sololife.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :fromMillis AND date <= :toMillis AND deletedAt IS NULL ORDER BY date DESC")
    fun getByDateRange(fromMillis: Long, toMillis: Long): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :fromMillis AND date <= :toMillis AND deletedAt IS NULL")
    fun sumByDateRange(fromMillis: Long, toMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :dayStart AND date <= :dayEnd AND deletedAt IS NULL")
    fun todayTotal(dayStart: Long, dayEnd: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE deletedAt IS NULL")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Query("""
        SELECT ((date + :tzOffsetMs) / 86400000) * 86400000 - :tzOffsetMs AS dayStart,
               COALESCE(SUM(amount), 0.0) AS total
        FROM expenses WHERE date >= :fromMillis AND date <= :toMillis AND deletedAt IS NULL
        GROUP BY (date + :tzOffsetMs) / 86400000
        ORDER BY dayStart ASC
    """)
    fun dailyTotals(fromMillis: Long, toMillis: Long, tzOffsetMs: Long): Flow<List<DailyTotal>>

    @Update
    suspend fun update(expense: Expense)

    @Query("SELECT * FROM expenses WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashed(): Flow<List<Expense>>

    @Query("UPDATE expenses SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("UPDATE expenses SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM expenses WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffMillis")
    suspend fun purgeExpiredTrash(cutoffMillis: Long)
}
