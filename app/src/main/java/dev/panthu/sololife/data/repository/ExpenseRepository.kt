package dev.panthu.sololife.data.repository

import androidx.room.withTransaction
import dev.panthu.sololife.data.db.DailyTotal
import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseDao
import dev.panthu.sololife.data.db.SoloLifeDatabase
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val db: SoloLifeDatabase) {
    private val dao: ExpenseDao = db.expenseDao()

    fun getAll(): Flow<List<Expense>> = dao.getAll()

    fun getByDateRange(fromMillis: Long, toMillis: Long): Flow<List<Expense>> =
        dao.getByDateRange(fromMillis, toMillis)

    fun weekTotal(weekStart: Long, weekEnd: Long): Flow<Double> =
        dao.sumByDateRange(weekStart, weekEnd)

    fun todayTotal(dayStart: Long, dayEnd: Long): Flow<Double> =
        dao.todayTotal(dayStart, dayEnd)

    suspend fun add(expense: Expense): Long = dao.insert(expense)

    suspend fun delete(expense: Expense) = dao.delete(expense)

    suspend fun softDelete(expense: Expense) = dao.softDelete(expense.id, System.currentTimeMillis())
    suspend fun restore(expense: Expense) = dao.restore(expense.id)
    fun getTrashed(): Flow<List<Expense>> = dao.getTrashed()
    suspend fun permanentDelete(expense: Expense) = dao.delete(expense)
    suspend fun purgeExpiredTrash() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.purgeExpiredTrash(cutoff)
    }

    suspend fun replaceAll(expenses: List<Expense>) {
        db.withTransaction {
            dao.deleteAll()
            dao.insertAll(expenses)
        }
    }

    suspend fun mergeAll(expenses: List<Expense>) = dao.insertAll(expenses)

    fun dailyTotals(fromMillis: Long, toMillis: Long): Flow<List<DailyTotal>> {
        val tzOffsetMs = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
        return dao.dailyTotals(fromMillis, toMillis, tzOffsetMs)
    }

    suspend fun update(expense: Expense) = dao.update(expense)
}
