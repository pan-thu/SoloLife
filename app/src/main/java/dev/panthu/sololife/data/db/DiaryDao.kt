package dev.panthu.sololife.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries WHERE deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Long): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') AND deletedAt IS NULL ORDER BY date DESC")
    fun search(query: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE deletedAt IS NULL ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE deletedAt IS NULL")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DiaryEntry>)

    @Query("SELECT date FROM diary_entries WHERE date >= :fromMillis AND date <= :toMillis AND deletedAt IS NULL")
    fun getEntryDatesInRange(fromMillis: Long, toMillis: Long): Flow<List<Long>>

    @Query("SELECT * FROM diary_entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashed(): Flow<List<DiaryEntry>>

    @Query("UPDATE diary_entries SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("UPDATE diary_entries SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM diary_entries WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffMillis")
    suspend fun purgeExpiredTrash(cutoffMillis: Long)
}
