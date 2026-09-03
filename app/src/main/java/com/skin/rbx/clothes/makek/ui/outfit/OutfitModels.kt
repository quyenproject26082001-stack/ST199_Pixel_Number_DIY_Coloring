package com.skin.rbx.clothes.makek.ui.outfit

import java.io.File

data class DownloadEntry(
    val type: String,
    val sourceUrl: String,
    val previewUrl: String = sourceUrl,
)

object OutfitUrls {
    const val INTERNAL_PREFIX = "https://appassets.androidplatform.net/internal/"
    const val ST253_API = "https://lvtglobal.tech/api/ST253_Clothes_Skins_Maker_for_RBX_v2"
    const val ST253_PUBLIC = "https://lvtglobal.tech/public/app/ST253_ClothesSkinsMakerforRBX_v2"

    fun accessoryPreview(modelUrl: String): String = modelUrl
        .replace("/3D/", "/2D/")
        .replace(Regex("\\.glb$", RegexOption.IGNORE_CASE), ".png")

    fun localFileName(url: String): String? =
        url.takeIf { it.startsWith(INTERNAL_PREFIX) }?.removePrefix(INTERNAL_PREFIX)

    fun glideSource(url: String, filesDir: File? = null): Any = when {
        url.startsWith(INTERNAL_PREFIX) && filesDir != null ->
            File(filesDir, "outfit/${url.removePrefix(INTERNAL_PREFIX)}")
        url.startsWith(INTERNAL_PREFIX) -> url
        url.startsWith("https://appassets.androidplatform.net/assets/") ->
            url.replace("https://appassets.androidplatform.net/assets/", "file:///android_asset/")
        else -> url
    }
}
