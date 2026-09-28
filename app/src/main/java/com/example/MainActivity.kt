// PHOTO SAVE - GALLERY FIX
private fun savePhotoToGallery(bitmap: Bitmap) {
    val filename = "UltraZoom_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/UltraZoom")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        contentResolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(it, values, null, null)
        Toast.makeText(this, "Gallery madhe save jhala: DCIM/UltraZoom", Toast.LENGTH_SHORT).show()
    }
}

// VIDEO SAVE - GALLERY FIX
private fun saveVideoToGallery(file: File) {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "UltraZoom_${System.currentTimeMillis()}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/UltraZoom")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        contentResolver.openOutputStream(it)?.use { out ->
            FileInputStream(file).use { input -> input.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(it, values, null, null)
        Toast.makeText(this, "Video Gallery madhe save jhala!", Toast.LENGTH_SHORT).show()
    }
}
