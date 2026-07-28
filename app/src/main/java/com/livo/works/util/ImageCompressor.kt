package com.livo.works.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object ImageCompressor {

    private const val TAG = "ImageCompressor"

    const val MAX_IMAGES = 10
    const val MAX_SOURCE_BYTES = 20L * 1024 * 1024   // 20 MB before compression
    const val MAX_COMPRESSED_BYTES = 5L * 1024 * 1024 // 5 MB after compression

    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 80
    private const val CACHE_DIR_NAME = "media_uploads"
    private const val STALE_FILE_AGE_MS = 6 * 60 * 60 * 1000L // 6 hours

    private val SUPPORTED_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    )

    fun validate(context: Context, uri: Uri) {
        val mimeType = context.contentResolver.getType(uri)?.lowercase()
            ?: throw MediaException("Could not read this file's type")

        if (mimeType !in SUPPORTED_TYPES) {
            throw MediaException("Unsupported format. Use JPEG, PNG or WebP")
        }

        val sizeBytes = querySize(context, uri)
        if (sizeBytes > MAX_SOURCE_BYTES) {
            throw MediaException("Image is larger than 20 MB")
        }
    }

    suspend fun compress(context: Context, uri: Uri, fileName: String): CompressedImage =
        withContext(Dispatchers.IO) {
            validate(context, uri)

            val resolver = context.contentResolver

            // Bounds pass: inJustDecodeBounds makes decodeStream return null on
            // purpose (it only fills in outWidth/outHeight), so the null check
            // belongs on the stream itself - never on the decode result.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = openInputStreamWithRetry(resolver, uri)
                ?: throw MediaException("Could not open this image")
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw MediaException("Could not read this image")
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            }

            // Pixel pass: here decodeStream really does return the Bitmap, so a
            // null result genuinely means the image could not be decoded.
            val pixelStream = openInputStreamWithRetry(resolver, uri)
                ?: throw MediaException("Could not open this image")
            var bitmap = pixelStream.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
                ?: throw MediaException("Could not decode this image")

            bitmap = scaleWithinBounds(bitmap)
            bitmap = applyExifOrientation(context, uri, bitmap)

            val outputFile = File(cacheDir(context), "$fileName.jpg")
            try {
                FileOutputStream(outputFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
            } finally {
                bitmap.recycle()
            }

            if (outputFile.length() > MAX_COMPRESSED_BYTES) {
                outputFile.delete()
                throw MediaException("Image is still larger than 5 MB after compression")
            }

            CompressedImage(
                file = outputFile,
                sizeBytes = outputFile.length(),
                contentType = "image/jpeg"
            )
        }

    fun cacheDir(context: Context): File =
        File(context.cacheDir, CACHE_DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * Compressed copies are normally deleted as soon as an image is removed or
     * the ViewModel clears, but a process death skips that and leaves copies of
     * the user's photos on disk. Only files too old to belong to a live upload
     * session are dropped, so a concurrent screen's in-flight upload is safe.
     */
    fun pruneStaleFiles(context: Context, maxAgeMs: Long = STALE_FILE_AGE_MS) {
        try {
            val cutoff = System.currentTimeMillis() - maxAgeMs
            cacheDir(context).listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoff) file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "pruneStaleFiles failed", e)
        }
    }

    /**
     * The Photo Picker's content provider can briefly return null right as it
     * hands control back to the app (seen in practice within ~100ms of the
     * picker closing, especially when multiple images are opened at once).
     * A couple of short retries clears it up without any user-visible delay
     * when it doesn't happen, and without hanging when the file is genuinely
     * unreadable.
     */
    private suspend fun openInputStreamWithRetry(
        resolver: ContentResolver,
        uri: Uri,
        attempts: Int = 3
    ): InputStream? {
        repeat(attempts) { attempt ->
            val stream = try {
                resolver.openInputStream(uri)
            } catch (e: Exception) {
                // Log rather than swallow: a SecurityException here means a lost
                // URI grant, which needs a very different fix than a retry.
                Log.w(TAG, "openInputStream failed for $uri (attempt ${attempt + 1})", e)
                null
            }
            if (stream != null) return stream
            if (attempt < attempts - 1) delay(150L * (attempt + 1))
        }
        return null
    }

    private fun querySize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) {
                    return cursor.getLong(index)
                }
            }

        // Fall back to the descriptor length when the provider omits SIZE.
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            ?.takeIf { it > 0 } ?: 0L
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= MAX_DIMENSION && currentHeight / 2 >= MAX_DIMENSION) {
            currentWidth /= 2
            currentHeight /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun scaleWithinBounds(source: Bitmap): Bitmap {
        val largestSide = maxOf(source.width, source.height)
        if (largestSide <= MAX_DIMENSION) return source

        val ratio = MAX_DIMENSION.toFloat() / largestSide
        val targetWidth = (source.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (source.height * ratio).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        if (scaled != source) source.recycle()
        return scaled
    }

    /**
     * Re-encoding drops EXIF, so the orientation is baked into the pixels here.
     */
    private suspend fun applyExifOrientation(context: Context, uri: Uri, source: Bitmap): Bitmap {
        val orientation = try {
            openInputStreamWithRetry(context.contentResolver, uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return source
        }

        return try {
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotated != source) source.recycle()
            rotated
        } catch (e: OutOfMemoryError) {
            source
        }
    }
}
