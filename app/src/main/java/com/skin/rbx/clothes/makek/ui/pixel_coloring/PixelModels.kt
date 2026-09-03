package com.skin.rbx.clothes.makek.ui.pixel_coloring

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class PixelLevelEntry(
    @SerializedName("id") val id: String,
    @SerializedName("category") val category: String,
    @SerializedName("preview") val preview: String? = null,
)

@Keep
data class PixelLevel(
    @SerializedName("id") val id: String,
    @SerializedName("category") val category: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("palette") val palette: List<String>,
    @SerializedName("grid") val grid: List<List<Int>>,
) {
    val totalPaintable: Int
        get() = grid.sumOf { row -> row.count { it != 0 } }
}

data class PixelPaletteItem(
    val colorId: Int,
    val color: Int,
    val percent: Int,
)

enum class PixelTool { NONE, WAND, BOMB }
