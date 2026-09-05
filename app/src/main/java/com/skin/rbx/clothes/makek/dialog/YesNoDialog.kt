package com.skin.rbx.clothes.makek.dialog
import com.skin.rbx.clothes.makek.core.helper.UnitHelper

import android.app.Activity
import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.hideNavigation
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseDialog
import com.skin.rbx.clothes.makek.core.extensions.strings
import com.skin.rbx.clothes.makek.databinding.DialogConfirmBinding

class YesNoDialog(
    val context: Activity,
    val title: Int,
    val description: Int,
    val isError: Boolean = false
) : BaseDialog<DialogConfirmBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_confirm
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: (() -> Unit) = {}
    var onDismissClick: (() -> Unit) = {}

    override fun initView() {
        initText()
        initBackground()
        if (isError) {
            binding.btnNo.gone()
            binding.btnYes.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = UnitHelper.dpToPxInt(context.resources, 144f)
            }
            binding.icYesbg.setImageResource(R.drawable.ic_no_bg)
        }
        context.hideNavigation()
        binding.tvTitle.isSelected = true
    }

    private fun initBackground() {
        binding.containerDialog.setBackgroundResource(R.drawable.bg_dl)
//        binding.btnNo.setBackgroundResource(R.drawable.ic_no_dialog)
//        binding.btnYes.setBackgroundResource(R.drawable.ic_yes_dialog)
        val paddingVertical = UnitHelper.dpToPxInt(context.resources, 9f)
        binding.btnNo.setPadding(0, paddingVertical, 0, paddingVertical)
        binding.btnYes.setPadding(0, paddingVertical, 0, paddingVertical)
    }

    override fun initAction() {
        binding.apply {
            btnNo.tap { onNoClick.invoke() }
            btnYes.tap { onYesClick.invoke() }
            flOutSide.tap { onDismissClick.invoke() }
        }
    }

    override fun onDismissListener() {}

    private fun initText() {
        binding.apply {
            tvTitle.text = context.strings(title)
            tvDescription.text = context.strings(description)
            if (isError) {
                btnYes.text = context.strings(R.string.ok)
            }
        }
    }
}
