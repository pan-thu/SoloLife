package dev.panthu.sololife.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

suspend fun saveImage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "diary_images").also { it.mkdirs() }
    val ext = uri.lastPathSegment
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?: "jpg"
    val dest = File(dir, "${UUID.randomUUID()}.$ext")
    context.contentResolver.openInputStream(uri)!!.use { input ->
        dest.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    dest.absolutePath
}

fun deleteImage(path: String) {
    File(path).delete()
}

fun pathToUri(context: Context, path: String): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))

fun parseUris(raw: String): List<String> =
    if (raw.isBlank()) emptyList() else raw.split("|").filter { it.isNotBlank() }

fun encodeUris(paths: List<String>): String = paths.joinToString("|")
