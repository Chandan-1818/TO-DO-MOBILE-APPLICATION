package com.chandan.master.presentation.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chandan.master.R
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Circular profile picture. [source] is a private file path or a content:// URI; null (or a failed load)
 * shows the default samurai avatar.
 */
@Composable
fun ProfileAvatar(size: Dp, modifier: Modifier = Modifier, source: String? = null) {
    val context = LocalContext.current
    val targetPx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, source, targetPx) {
        value = if (source == null) null else withContext(Dispatchers.IO) { decodeAvatar(context, source, targetPx) }
    }
    val shapeModifier = modifier
        .size(size)
        .clip(CircleShape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded, stringResource(R.string.profile_picture_description), shapeModifier, contentScale = ContentScale.Crop)
    } else {
        Image(painterResource(R.drawable.default_avatar), stringResource(R.string.profile_picture_description), shapeModifier, contentScale = ContentScale.Crop)
    }
}

private fun decodeAvatar(context: Context, source: String, targetPx: Int): ImageBitmap? = try {
    fun open(): InputStream? =
        if (source.startsWith("content://")) context.contentResolver.openInputStream(Uri.parse(source)) else File(source).inputStream()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
    open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }?.asImageBitmap()
} catch (e: Exception) {
    null
}
