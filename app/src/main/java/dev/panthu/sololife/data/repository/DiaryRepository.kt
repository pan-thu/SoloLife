package dev.panthu.sololife.data.repository

import dev.panthu.sololife.data.db.DiaryDao
import dev.panthu.sololife.data.db.DiaryEntry
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val dao: DiaryDao) {

    fun getAll(): Flow<List<DiaryEntry>> = dao.getAll()

    fun search(query: String): Flow<List<DiaryEntry>> = dao.search(query)

    suspend fun getById(id: Long): DiaryEntry? = dao.getById(id)

    suspend fun getLatest(): DiaryEntry? = dao.getLatest()

    suspend fun save(entry: DiaryEntry): Long = dao.insert(entry)

    suspend fun update(entry: DiaryEntry) = dao.update(entry)

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)

    suspend fun replaceAll(entries: List<DiaryEntry>) {
        dao.deleteAll()
        dao.insertAll(entries)
    }

    suspend fun mergeAll(entries: List<DiaryEntry>) = dao.insertAll(entries)

    fun getEntryDatesInRange(fromMillis: Long, toMillis: Long): Flow<List<Long>> =
        dao.getEntryDatesInRange(fromMillis, toMillis)
}
