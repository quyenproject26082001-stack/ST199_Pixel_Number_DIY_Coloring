package com.skin.rbx.clothes.makek.ui.clothes

object ClothesMode {
    const val SPECIAL = "special"
    const val ACCESSORY = "accessory"
    const val BASIC_OUTFIT = "basic_outfit"
    const val BASIC_SHIRT = "basic_shirt"
    const val BASIC_PANT = "basic_pant"
}

data class ClothesListItem(
    val previewUrl: String,
    val shirtUrl: String? = null,
    val pantUrl: String? = null,
    val accessoryType: String? = null,
    val accessoryUrl: String? = null,
)

data class ClothesCatalogResponse(
    val folders: List<ClothesFolder> = emptyList(),
    val models: Map<String, List<String>> = emptyMap(),
)

data class ClothesFolder(
    val category: String,
    val quantity: Int,
)
