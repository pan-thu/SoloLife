package dev.panthu.sololife.data.repository

import dev.panthu.sololife.data.db.Expense
import dev.panthu.sololife.data.db.ExpenseDao
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {

    fun getAll(): Flow<List<Expense>> = dao.getAll()

    fun getByDateRange(fromMillis: Long, toMillis: Long): Flow<List<Expense>> =
        dao.getByDateRange(fromMillis, toMillis)

    fun weekTotal(weekStart: Long, weekEnd: Long): Flow<Double> =
        dao.sumByDateRange(weekStart, weekEnd)

    fun todayTotal(dayStart: Long, dayEnd: Long): Flow<Double> =
        dao.todayTotal(dayStart, dayEnd)

    suspend fun add(expense: Expense): Long = dao.insert(expense)

    suspend fun delete(expense: Expense) = dao.delete(expense)

    suspend fun replaceAll(expenses: List<Expense>) {
        dao.deleteAll()
        dao.insertAll(expenses)
    }

    suspend fun mergeAll(expenses: List<Expense>) = dao.insertAll(expenses)
}
