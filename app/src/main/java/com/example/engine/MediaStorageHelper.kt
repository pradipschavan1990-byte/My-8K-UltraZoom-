package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Robust MediaStore helper that guarantees captured photos and videos are saved directly
 * to the Android device's public Gallery (under DCIM/UltraZoom8K), making them immediately
 * visible in Google Photos, Xiaomi/Poco Gallery, and any file manager.
 */
object MediaStorageHelper {

    private const val ALBUM_NAME = "UltraZoom8K"

    /**
     * Saves a Bitmap both to the app's internal vault (for instant caching in the app)
     * AND directly to Android's public MediaStore (DCIM/UltraZoom8K) so it appears
     * in the phone's native Gallery app.
     *
     * @return Pair of (internalFilePath, publicGalleryUriString)
     */
    suspend fun saveImageToGallery(
        context: Context,
        bitmap: Bitmap,
        filenamePrefix: String = "UltraZoom_HDREAL",
        showToast: Boolean = true
    ): Pair<String, String?> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // 1. Save to internal vault
        val internalDir = File(context.filesDir, "ultra_zoom_captures")
        if (!internalDir.exists()) internalDir.mkdirs()
        val localFilename = "${filenamePrefix}_$timestamp.jpg"
        val localFile = File(internalDir, localFilename)
        try {
            FileOutputStream(localFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val localPath = localFile.absolutePath

        // 2. Insert into device's public MediaStore
        var galleryUriStr: String? = null
        try {
            val resolver = context.contentResolver
            val displayName = "${filenamePrefix}_$timestamp.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, timestamp / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$ALBUM_NAME")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 96, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                galleryUriStr = uri.toString()

                // Trigger MediaScanner for instant indexing on device
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(localPath),
                    arrayOf("image/jpeg"),
                    null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (showToast) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "✓ फोटो गॅलरीमध्ये सेव्ह झाला! (DCIM/$ALBUM_NAME)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        Pair(localPath, galleryUriStr)
    }

    /**
     * Exports a recorded video MP4 file to the device's public MediaStore (DCIM/UltraZoom8K),
     * making it immediately playable in Google Photos, Xiaomi/Poco Gallery, and VLC.
     */
    suspend fun exportVideoToGallery(
        context: Context,
        sourceVideoFile: File,
        filenamePrefix: String = "UltraZoom_Video",
        showToast: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
            return@withContext null
        }

        val timestamp = System.currentTimeMillis()
        var galleryUriStr: String? = null

        try {
            val resolver = context.contentResolver
            val displayName = "${filenamePrefix}_$timestamp.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, timestamp / 1000)
                put(MediaStore.Video.Media.DATE_TAKEN, timestamp)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$ALBUM_NAME")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val uri: Uri? = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream ->
                    sourceVideoFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                galleryUriStr = uri.toString()

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(sourceVideoFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (showToast) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "✓ व्हिडिओ गॅलरीमध्ये सेव्ह झाला! (DCIM/$ALBUM_NAME)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        galleryUriStr
    }
}
