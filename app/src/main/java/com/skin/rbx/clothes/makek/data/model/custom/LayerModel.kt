package com.skin.rbx.clothes.makek.data.model.custom

import com.skin.rbx.clothes.makek.data.model.custom.ColorModel

data class LayerModel(
    val image: String,
    val isMoreColors: Boolean = false,
    var listColor: ArrayList<ColorModel> = arrayListOf(),
    val thumb: String = ""
)