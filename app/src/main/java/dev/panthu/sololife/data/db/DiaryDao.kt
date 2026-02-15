package dev.panthu.sololife.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Long): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY date DESC")
    fun search(query: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DiaryEntry>)
}
