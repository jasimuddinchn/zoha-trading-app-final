package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ReceiptImageHelper {
    private const val TAG = "ReceiptImageHelper"
    private const val MAX_SIZE_BYTES = 150 * 1024 // 150 KB
    private const val MAX_DIMENSION = 1280 // Max width/height to avoid memory bloat and keep under 150KB

    /**
     * Compresses an image picked by Uri down to max ~150KB (JPEG 70-80%).
     * Returns ByteArray of the compressed JPEG.
     */
    suspend fun compressImage(context: Context, imageUri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) {
            throw IllegalArgumentException("Unable to decode image from uri: $imageUri")
        }

        // Handle rotation if needed from EXIF
        val orientedBitmap = fixOrientation(context, imageUri, originalBitmap)

        // Downscale if width or height exceeds MAX_DIMENSION
        val scaledBitmap = scaleBitmapDown(orientedBitmap, MAX_DIMENSION)

        // Compress iteratively to ensure size is <= MAX_SIZE_BYTES
        var quality = 80
        var outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        while (outputStream.size() > MAX_SIZE_BYTES && quality > 40) {
            outputStream.reset()
            quality -= 10
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        // If still over 150KB, scale down further and compress at 70%
        if (outputStream.size() > MAX_SIZE_BYTES) {
            val halfBitmap = Bitmap.createScaledBitmap(
                scaledBitmap,
                (scaledBitmap.width * 0.75).toInt().coerceAtLeast(100),
                (scaledBitmap.height * 0.75).toInt().coerceAtLeast(100),
                true
            )
            outputStream.reset()
            halfBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        }

        val resultBytes = outputStream.toByteArray()
        Log.d(TAG, "Compressed receipt image: original size -> ${resultBytes.size / 1024} KB at quality $quality")
        resultBytes
    }

    /**
     * Compresses a Bitmap directly to max ~150KB (JPEG 70-80%).
     */
    suspend fun compressBitmap(bitmap: Bitmap): ByteArray = withContext(Dispatchers.IO) {
        val scaledBitmap = scaleBitmapDown(bitmap, MAX_DIMENSION)
        var quality = 80
        var outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        while (outputStream.size() > MAX_SIZE_BYTES && quality > 40) {
            outputStream.reset()
            quality -= 10
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        outputStream.toByteArray()
    }

    /**
     * Uploads the compressed bytes to Firebase Storage under `/expense_receipts/`.
     * Falls back to saving locally in context.filesDir/receipts/ if storage fails.
     * Returns the remote download URL (or local file URI fallback).
     */
    suspend fun uploadReceiptImage(
        context: Context,
        compressedBytes: ByteArray,
        expenseTitle: String = "expense"
    ): String = withContext(Dispatchers.IO) {
        val fileName = "receipt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"

        // 1. Always save a local copy as an offline-first backup and fast cache
        val localFile = saveLocalReceiptBackup(context, fileName, compressedBytes)
        val localUriString = Uri.fromFile(localFile).toString()

        // 2. Upload to Firebase Storage under /expense_receipts/
        try {
            val storageRef = FirebaseStorage.getInstance().reference.child("expense_receipts/$fileName")
            val uploadTask = storageRef.putBytes(compressedBytes).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Successfully uploaded receipt to Firebase Storage: $downloadUrl")
            downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload failed (${e.message}), using local persistent cache: $localUriString")
            localUriString
        }
    }

    private fun saveLocalReceiptBackup(context: Context, fileName: String, bytes: ByteArray): File {
        val dir = File(context.filesDir, "expense_receipts").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth.coerceAtLeast(1), newHeight.coerceAtLeast(1), true)
    }

    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.w(TAG, "Exif orientation read failed: ${e.message}")
            bitmap
        }
    }
}
