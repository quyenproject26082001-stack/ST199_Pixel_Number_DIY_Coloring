package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.app.Activity
import android.graphics.Bitmap
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseDialog
import com.skin.rbx.clothes.makek.core.extensions.hideNavigation
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.databinding.DialogRetryBinding

class PixelRetryDialog(
    private val activity: Activity,
    private val preview: Bitmap,
) : BaseDialog<DialogRetryBinding>(activity, maxWidth = true, maxHeight = true) {
    override val layoutId = R.layout.dialog_retry
    override val isCancelOnTouchOutside = false
    override val isCancelableByBack = true

    var onRetryClick: () -> Unit = {}
    var onShareClick: () -> Unit = {}

    override fun initView() {
        binding.imvPixel.setPreviewBitmap(preview)
        activity.hideNavigation()
    }

    override fun initAction() = with(binding) {
        btnClose.tap { dismiss() }
        btnRetry.tap { onRetryClick() }
        btnShare.tap { onShareClick() }
    }

    override fun onDismissListener() = Unit
}
