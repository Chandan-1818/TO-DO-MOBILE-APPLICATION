package com.chandan.master.data.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.chandan.master.domain.repository.AvatarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AvatarStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : AvatarRepository {
    private val dir get() = File(context.filesDir, "avatars").also { it.mkdirs() }

    override suspend fun importFromUri(uri: String): String? = withContext(Dispatchers.IO) {
        try {
            val parsed = Uri.parse(uri)
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= TARGET) sample *= 2
            val decoded = resolver.openInputStream(parsed)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null

            val rotation = resolver.openInputStream(parsed)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val side = minOf(decoded.width, decoded.height)
            val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
            val matrix = Matrix().apply {
                postRotate(rotation)
                val scale = minOf(1f, TARGET.toFloat() / side)
                postScale(scale, scale)
            }
            val result = Bitmap.createBitmap(square, 0, 0, square.width, square.height, matrix, true)

            val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun delete(path: String?) {
        if (path == null) return
        withContext(Dispatchers.IO) {
            val file = File(path)
            // Only ever delete inside our own avatar directory.
            if (file.canonicalFile.parentFile == dir.canonicalFile) file.delete()
        }
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) { dir.listFiles()?.forEach { it.delete() } }
    }

    private companion object { const val TARGET = 512 }
}
