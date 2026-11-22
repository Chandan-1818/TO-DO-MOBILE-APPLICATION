package com.chandan.master.data.backup

import android.content.Context
import android.net.Uri
import com.chandan.master.domain.backup.BackupCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads and writes backup files the user chose through the Storage Access Framework (no storage permission needed). */
@Singleton
class BackupFiles @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // "wt" truncates: a shorter backup never leaves stale bytes behind an older, longer file.
            context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        } catch (e: Exception) { false }
    }

    /** Returns null if unreadable; reads at most MAX_BYTES+1 bytes so a huge wrong file cannot exhaust memory. */
    suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val limit = BackupCodec.MAX_BYTES + 1
                val buffer = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                var total = 0
                while (total < limit) {
                    val n = input.read(chunk, 0, minOf(chunk.size, limit - total))
                    if (n < 0) break
                    buffer.write(chunk, 0, n); total += n
                }
                buffer.toString(Charsets.UTF_8.name())
            }
        } catch (e: Exception) { null }
    }
}
