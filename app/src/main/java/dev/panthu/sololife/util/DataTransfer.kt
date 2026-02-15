package dev.panthu.sololife.util

import android.content.Context
import android.net.Uri
import dev.panthu.sololife.data.db.DiaryEntry
import dev.panthu.sololife.data.db.Expense
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val diary: List<DiaryEntry> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

object DataTransfer {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun export(context: Context, uri: Uri, diary: List<DiaryEntry>, expenses: List<Expense>): Result<Unit> {
        return runCatching {
            val backup = AppBackup(diary = diary, expenses = expenses)
            val jsonString = json.encodeToString(backup)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open output stream")
        }
    }

    fun import(context: Context, uri: Uri): Result<AppBackup> {
        return runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: error("Could not open input stream")
            json.decodeFromString<AppBackup>(text)
        }
    }
}
