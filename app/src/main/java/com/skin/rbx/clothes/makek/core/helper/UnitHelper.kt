package com.skin.rbx.clothes.makek.core.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

object UnitHelper {
    fun dpToPx(resources: Resources, dp: Float): Float {
        return dp * tabletDpScale(resources) * resources.displayMetrics.density
    }

    fun dpToPxInt(resources: Resources, dp: Float): Int {
        val px = dpToPx(resources, dp)
        return when {
            px > 0 -> (px + 0.5f).toInt()
            px < 0 -> (px - 0.5f).toInt()
            else -> 0
        }
    }

    fun spToPx(resources: Resources, sp: Float): Float {
        return sp * tabletSpScale(resources) * resources.displayMetrics.scaledDensity
    }

    fun spToPx(spVal: Float): Int {
        val r = Resources.getSystem()
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spVal * tabletSpScale(r),
            r.displayMetrics,
        ).toInt()
    }

    fun pxToDpInt(context: Context, px: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            px * tabletDpScale(context.resources),
            context.resources.displayMetrics,
        ).toInt()
    }

    fun pxToDpFloat(context: Context, px: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            px * tabletDpScale(context.resources),
            context.resources.displayMetrics,
        )
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * tabletDpScale(context.resources) * context.resources.displayMetrics.density).toInt()
    }
    fun dpToPx(context: Context, dp: Float): Float {
        return dp * tabletDpScale(context.resources) * context.resources.displayMetrics.density
    }

    private fun tabletDpScale(resources: Resources): Float =
        if (resources.configuration.smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP) 2f else 1f

    private fun tabletSpScale(resources: Resources): Float =
        if (resources.configuration.smallestScreenWidthDp >= TABLET_MIN_WIDTH_DP) 1.7f else 1f

    private const val TABLET_MIN_WIDTH_DP = 600
    @SuppressLint("DefaultLocale")
    fun formatFileSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes Bytes"
            sizeInBytes < 1024 * 1024 -> String.format("%.1f KB", sizeInBytes / 1024.0)
            sizeInBytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", sizeInBytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", sizeInBytes / (1024.0 * 1024 * 1024))
        }
    }


}
