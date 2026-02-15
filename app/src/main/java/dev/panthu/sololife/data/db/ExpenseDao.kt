package dev.panthu.sololife.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :fromMillis AND date <= :toMillis ORDER BY date DESC")
    fun getByDateRange(fromMillis: Long, toMillis: Long): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :fromMillis AND date <= :toMillis")
    fun sumByDateRange(fromMillis: Long, toMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :dayStart AND date <= :dayEnd")
    fun todayTotal(dayStart: Long, dayEnd: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)
}
