package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

internal fun createCompletedPreview(level: PixelLevel): Bitmap =
    renderPixelPreview(level = level, showCompleted = true, targetSize = 512)

internal fun saveShareArtwork(context: Context, bitmap: Bitmap, levelId: String): String? =
    runCatching {
        val directory = File(context.filesDir, "pixel_coloring_share").apply { mkdirs() }
        val safeName = levelId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(directory, "$safeName.png")
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        file.absolutePath
    }.getOrNull()
