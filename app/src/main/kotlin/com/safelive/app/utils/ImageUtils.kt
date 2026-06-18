package com.safelive.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = File(context.cacheDir, "images").also { it.mkdirs() }
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        } catch (e: IOException) {
            Timber.e(e, "Failed to create image file")
            null
        }
    }

    fun compressImage(uri: Uri, quality: Int = Constants.IMAGE_QUALITY): File? {
        return try {
            val bitmap = decodeSampledBitmap(uri, 1280, 1280) ?: return null
            try {
                val outputFile = createImageFile() ?: return null
                var currentQuality = quality.coerceIn(10, 100)
                val maxSizeBytes = Constants.MAX_IMAGE_SIZE_MB * 1024 * 1024

                do {
                    FileOutputStream(outputFile, false).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
                        outputStream.flush()
                    }

                    if (outputFile.length() <= maxSizeBytes || currentQuality <= 10) {
                        break
                    }
                    currentQuality -= 10
                } while (true)

                outputFile
            } finally {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to compress image")
            null
        }
    }

    private fun decodeSampledBitmap(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, bounds)
        }

        bounds.inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
        bounds.inJustDecodeBounds = false
        bounds.inPreferredConfig = Bitmap.Config.RGB_565

        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, bounds)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun createMultipartFromUri(uri: Uri, fieldName: String = "images"): MultipartBody.Part? {
        val compressedFile = compressImage(uri) ?: return null
        val requestBody = compressedFile.asRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(fieldName, compressedFile.name, requestBody)
    }

    fun createMultipartFromFile(file: File, fieldName: String = "images"): MultipartBody.Part {
        val requestBody = file.asRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(fieldName, file.name, requestBody)
    }

    fun clearImageCache() {
        try {
            val cacheDir = File(context.cacheDir, "images")
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear image cache")
        }
    }
}
