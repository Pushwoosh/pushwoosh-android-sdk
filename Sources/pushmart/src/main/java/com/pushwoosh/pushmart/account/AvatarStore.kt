package com.pushwoosh.pushmart.account

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.pushwoosh.internal.utils.PWLog
import java.io.File
import java.io.FileOutputStream

/**
 * The profile photo, kept on the device only as a [SIDE] px square JPEG, as iOS `UserView`
 * (`pushmart.avatar`). No SDK call is involved. A [load] during a [save] waits for it, so a screen
 * created meanwhile (after a rotation) shows the new photo.
 */
class AvatarStore(private val file: File) {
    @Synchronized
    fun load(): Bitmap? = if (file.exists()) BitmapFactory.decodeFile(file.path) else null

    /**
     * Decodes the picked image at [uri], centre-crops it to a square and stores it. Blocking: call
     * it off the main thread. Returns false when the image can't be read or stored.
     */
    @Synchronized
    fun save(resolver: ContentResolver, uri: Uri): Boolean {
        return try {
            val square = decodeSquare(resolver, uri) ?: return false
            val temp = File(file.path + ".tmp")
            val written =
                FileOutputStream(temp).use {
                    square.compress(Bitmap.CompressFormat.JPEG, QUALITY, it)
                }
            square.recycle()
            written && temp.renameTo(file)
        } catch (e: Exception) {
            PWLog.warn(TAG, "Couldn't store the profile photo: " + e.message)
            false
        } catch (e: OutOfMemoryError) {
            PWLog.warn(TAG, "The profile photo is too large: " + e.message)
            false
        }
    }

    @Synchronized
    fun remove() {
        file.delete()
    }

    private fun decodeSquare(resolver: ContentResolver, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val shortSide = minOf(bounds.outWidth, bounds.outHeight)
        if (shortSide <= 0) return null
        var sample = 1
        while (shortSide / (sample * 2) >= SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded =
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
        val side = minOf(decoded.width, decoded.height)
        val matrix = orientation(resolver, uri)
        matrix.preScale(SIDE.toFloat() / side, SIDE.toFloat() / side)
        val square =
            Bitmap.createBitmap(
                decoded,
                (decoded.width - side) / 2,
                (decoded.height - side) / 2,
                side,
                side,
                matrix,
                true)
        if (square !== decoded) decoded.recycle()
        return square
    }

    /** Camera photos keep their rotation in EXIF, which BitmapFactory ignores. */
    private fun orientation(resolver: ContentResolver, uri: Uri): Matrix {
        val matrix = Matrix()
        val orientation =
            try {
                resolver.openInputStream(uri)?.use {
                    ExifInterface(it)
                        .getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (e: Exception) {
                null
            }
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        }
        return matrix
    }

    companion object {
        const val SIDE = 320
        private const val QUALITY = 85
        private const val TAG = "AvatarStore"
    }
}
