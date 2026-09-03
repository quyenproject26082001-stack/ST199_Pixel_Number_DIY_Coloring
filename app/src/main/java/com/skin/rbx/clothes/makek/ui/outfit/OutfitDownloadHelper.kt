package com.skin.rbx.clothes.makek.ui.outfit

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.bumptech.glide.Glide
import java.io.File

object OutfitDownloadHelper {
    suspend fun save(context: Context, entry: DownloadEntry): Boolean = runCatching {
        val bitmap = OutfitUrls.localFileName(entry.previewUrl)?.let { fileName ->
            Glide.with(context).asBitmap().load(File(context.filesDir, "outfit/$fileName")).submit().get()
        } ?: run {
            val source = if (entry.previewUrl.startsWith("https://appassets.androidplatform.net/assets/")) {
                entry.previewUrl.replace("https://appassets.androidplatform.net/assets/", "file:///android_asset/")
            } else entry.previewUrl
            Glide.with(context).asBitmap().load(source).submit().get()
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${entry.type}_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Clothes Skins Maker")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Cannot create media file")
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ?: error("Cannot write media file")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrDefault(false)
}
