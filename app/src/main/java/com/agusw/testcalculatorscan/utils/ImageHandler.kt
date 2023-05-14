package com.agusw.testcalculatorscan.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore.Images.Media.ORIENTATION
import java.io.File
import java.io.FileNotFoundException


object ImageHandler {
    private const val DEFAULT_MIN_WIDTH_QUALITY = 680 // min pixels

    private const val TAG = "ImagePicker"
    private const val TEMP_IMAGE_NAME = "tempImage"

    fun getImageFromResult(context: Context, resultCode: Int, intent: Intent?): Bitmap? {
        val imageFile = getTempFile(context)
        if (resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri
            val isCamera = intent?.data?.equals(Uri.fromFile(imageFile)) ?: false

            selectedImage = if (isCamera)
                Uri.fromFile(imageFile)
            else
                intent?.data ?: return null

            val bm = getImageResized(context, selectedImage) ?: return null
            val rotation = getRotation(context, selectedImage, isCamera)
            return rotate(bm, rotation)
        }
        return null
    }

    fun processImageResult(context: Context, uri: Uri?, isCamera: Boolean): Bitmap? {
        if (uri == null) return null

        val bm = getImageResized(context, uri) ?: return null
        val rotation = getRotation(context, uri, isCamera)
        return rotate(bm, rotation)
    }

    private fun getImageResized(context: Context, selectedImage: Uri): Bitmap? {
        var bm: Bitmap?
        val sampleSizes = intArrayOf(5, 3, 2, 1)
        var i = 0
        do {
            bm = decodeBitmap(context, selectedImage, sampleSizes[i])
            i++
        } while ((bm?.width ?: 0) < DEFAULT_MIN_WIDTH_QUALITY && i < sampleSizes.size)

        return bm
    }

    private fun decodeBitmap(context: Context, uri: Uri, size: Int): Bitmap? {
        return try {
            val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")

            val bitmap = BitmapFactory.decodeFileDescriptor(
                fileDescriptor?.fileDescriptor,
                null,
                BitmapFactory.Options().apply { inSampleSize = size }
            )

            fileDescriptor?.close()

            bitmap
        } catch (e: FileNotFoundException) {
            null
        }
    }

    private fun getRotation(context: Context, imageUri: Uri, isCamera: Boolean = true) =
        if (isCamera) getRotationFromCamera(context, imageUri)
        else getRotationFromGallery(context, imageUri)

    private fun getRotationFromCamera(context: Context, imageUri: Uri): Int {
        try {
            context.contentResolver.notifyChange(imageUri, null)
            val exif = imageUri.path?.let { ExifInterface(it) } ?: return 0
            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_270 -> return 270
                ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                ExifInterface.ORIENTATION_ROTATE_90 -> return 90
            }
        } catch (_: Exception) {
        }

        return 0
    }

    @SuppressLint("Range")
    private fun getRotationFromGallery(context: Context, imageUri: Uri): Int {
        val cursor = context.contentResolver.query(
            imageUri,
            arrayOf(ORIENTATION),
            null,
            null
        ) ?: return 0

        return cursor.getInt(cursor.getColumnIndex(ORIENTATION)).also { cursor.close() }
    }

    private fun rotate(bm: Bitmap, rotation: Int) = bm.let {
        if (rotation != 0) {
            Bitmap.createBitmap(
                it,
                0,
                0,
                it.width,
                it.height,
                Matrix().apply { postRotate(rotation.toFloat()) },
                true
            )
        } else
            it
    }

    private fun getTempFile(context: Context) =
        File(context.externalCacheDir, TEMP_IMAGE_NAME).also { it.parentFile?.mkdirs() }
}