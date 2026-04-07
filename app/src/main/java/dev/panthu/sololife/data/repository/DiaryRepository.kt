package dev.panthu.sololife.data.repository

import androidx.room.withTransaction
import dev.panthu.sololife.data.db.DiaryDao
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.db.SoloLifeDatabase
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val db: SoloLifeDatabase) {
    private val dao: DiaryDao = db.diaryDao()

    fun getAll(): Flow<List<DiaryEntry>> = dao.getAll()

    fun search(query: String): Flow<List<DiaryEntry>> = dao.search(query)

    suspend fun getById(id: Long): DiaryEntry? = dao.getById(id)

    suspend fun getLatest(): DiaryEntry? = dao.getLatest()

    suspend fun save(entry: DiaryEntry): Long = dao.insert(entry)

    suspend fun update(entry: DiaryEntry) = dao.update(entry)

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)

    suspend fun softDelete(entry: DiaryEntry) = dao.softDelete(entry.id, System.currentTimeMillis())
    suspend fun restore(entry: DiaryEntry) = dao.restore(entry.id)
    fun getTrashed(): Flow<List<DiaryEntry>> = dao.getTrashed()
    suspend fun permanentDelete(entry: DiaryEntry) = dao.delete(entry)
    suspend fun purgeExpiredTrash() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.purgeExpiredTrash(cutoff)
    }

    suspend fun replaceAll(entries: List<DiaryEntry>) {
        db.withTransaction {
            dao.deleteAll()
            dao.insertAll(entries)
        }
    }

    suspend fun mergeAll(entries: List<DiaryEntry>) = dao.insertAll(entries)

    fun getEntryDatesInRange(fromMillis: Long, toMillis: Long): Flow<List<Long>> =
        dao.getEntryDatesInRange(fromMillis, toMillis)
}
