package com.skin.rbx.clothes.makek.core.utils

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.skin.rbx.clothes.makek.R
//import com.skin.rbx.clothes.makek.core.custom.layout.LayoutPresets
import com.skin.rbx.clothes.makek.data.model.IntroModel
import com.skin.rbx.clothes.makek.data.model.LanguageModel
import com.skin.rbx.clothes.makek.data.model.custom.CustomizeModel
import com.facebook.shimmer.Shimmer
import com.skin.rbx.clothes.makek.data.model.SelectedModel
import com.skin.rbx.clothes.makek.data.model.custom.NavigationModel

object DataLocal {
    val shimmer =
        Shimmer.ColorHighlightBuilder()
            .setHighlightColor(0xFFFFFFFF.toInt())  // màu highlight
            .setBaseColor(0xFF4A4A4A.toInt())       // màu nền
            .setDuration(1800)
            .setAutoStart(true)
            .build()

    var lastClickTime = 0L
    var currentDate = ""
    var isConnectInternet = MutableLiveData<Boolean>()
    var isFailBaseURL = false
    var isCallDataAlready = false

    fun getLanguageList(): ArrayList<LanguageModel> {
        return arrayListOf(
            LanguageModel("hi", "Hindi", R.drawable.ic_flag_hindi),
            LanguageModel("es", "Spanish", R.drawable.ic_flag_spanish),
            LanguageModel("fr", "French", R.drawable.ic_flag_french),
            LanguageModel("en", "English", R.drawable.ic_flag_english),
            LanguageModel("pt", "Portuguese", R.drawable.ic_flag_portugeese),
            LanguageModel("in", "Indonesian", R.drawable.ic_flag_indo),
            LanguageModel("de", "German", R.drawable.ic_flag_germani),
        )
    }

    val itemIntroList = listOf(
        IntroModel(R.drawable.img_intro_1, R.string.title_1),
        IntroModel(R.drawable.img_intro_2, R.string.title_2),
        IntroModel(R.drawable.img_intro_3, R.string.title_3)
    )

    fun getBackgroundColorDefault(context: Context): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_1)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_2)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_3)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_4)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_5)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_6)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_7)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_8)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_9)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_10)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_11)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_12)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_13)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_14)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_15)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_16)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_17)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_18)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_19)),
        )
    }

    val bottomNavigationNotSelect = arrayListOf(
        R.drawable.ic_background,
        R.drawable.ic_sticker,
        R.drawable.ic_speech,
        R.drawable.ic_text,
    )

    val bottomNavigationSelected = arrayListOf(
        R.drawable.ic_background_selected,
        R.drawable.ic_sticker_selected,
        R.drawable.ic_speech_selected,
        R.drawable.ic_text_selected,
    )

    fun getTextFontDefault(): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(value = R.font.roboto_regular),
            SelectedModel(value = R.font.aldrich),
            SelectedModel(value = R.font.brush_script),
            SelectedModel(value = R.font.nova_script),
            SelectedModel(value = R.font.carattere),
            SelectedModel(value = R.font.digital_numbers),
            SelectedModel(value = R.font.dynalight),
            SelectedModel(value = R.font.edwardian_script_itc),
            SelectedModel(value = R.font.vni_ongdo)
        )
    }

    fun getTextColorDefault(context: Context): ArrayList<SelectedModel> {
        return arrayListOf(
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_9)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.black)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.white)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_19)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_2)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_3)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_4)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_5)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_6)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_7)),
            SelectedModel(value = ContextCompat.getColor(context, R.color.color_8))
        )
    }
}