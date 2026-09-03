package com.skin.rbx.clothes.makek.dialog
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseDialog
import com.skin.rbx.clothes.makek.core.extensions.select
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.databinding.DialogColorPickerBinding


class ChooseColorDialog(context: Context) : BaseDialog<DialogColorPickerBinding>(context,maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_color_picker
    override val isCancelOnTouchOutside: Boolean =false
    override val isCancelableByBack: Boolean = false

    var onDoneEvent: ((Int) -> Unit) = {}
    var onCloseEvent: (() -> Unit) = {}
    var onDismissEvent: (() -> Unit) = {}
    private var color = Color.WHITE
    override fun initView() {
        binding.apply {

            btnDoneText.select()

            colorPickerView.apply {
                hueSliderView = hueSlider

                // Apply rounded corners programmatically
                val radiusPx = UnitHelper.dpToPx(context.resources, 8f)
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }
                clipToOutline = true
            }
        }
    }

    override fun initAction() {
        binding.apply {
            colorPickerView.setOnColorChangedListener {
                color = it
                // Update the color string display in real-time
             tvColorString.text = String.format("#%06X", 0xFFFFFF and it)
            }
            btnClose.tap {
                onCloseEvent.invoke()
                dismiss()
            }
            btnDone.tap { onDoneEvent.invoke(color) }
        }
    }

    override fun onDismissListener() {
        onDismissEvent.invoke()
    }

}
