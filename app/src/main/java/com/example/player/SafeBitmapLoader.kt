package com.example.player

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.io.InputStream
import java.util.concurrent.Executors

@UnstableApi
class SafeBitmapLoader(
    private val context: Context,
    private val maxDimensionPx: Int = 256,
) : BitmapLoader {

    private val executor: ListeningExecutorService =
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return executor.submit<Bitmap> {
            decodeSampledBitmapFromBytes(data, maxDimensionPx)
                ?: throw IllegalArgumentException("Failed to decode artwork bytes")
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return executor.submit<Bitmap> {
            loadSampledBitmapFromUri(uri, maxDimensionPx)
                ?: loadDefaultDrawableBitmap(maxDimensionPx)
                ?: throw IllegalArgumentException("Failed to load artwork from $uri")
        }
    }

    private fun loadDefaultDrawableBitmap(reqSize: Int): Bitmap? {
        return try {
            val defaultUri = Uri.parse(
                "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${com.example.R.drawable.img_default_album_cover}"
            )
            loadSampledBitmapFromUri(defaultUri, reqSize)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmapFromBytes(data: ByteArray, reqSize: Int): Bitmap? {
        if (data.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqSize)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(data, 0, data.size, options)
    }

    private fun loadSampledBitmapFromUri(uri: Uri, reqSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openUriStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqSize)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return openUriStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun openUriStream(uri: Uri): InputStream? {
        return try {
            when (uri.scheme) {
                ContentResolver.SCHEME_ANDROID_RESOURCE -> {
                    val id = uri.lastPathSegment?.toIntOrNull()
                    if (id != null) {
                        context.resources.openRawResource(id)
                    } else {
                        context.contentResolver.openInputStream(uri)
                    }
                }
                else -> context.contentResolver.openInputStream(uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqSize: Int): Int {
        var inSampleSize = 1
        while ((width / inSampleSize) > reqSize * 2 || (height / inSampleSize) > reqSize * 2) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
