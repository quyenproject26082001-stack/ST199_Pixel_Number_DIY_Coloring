package com.skin.rbx.clothes.makek.listener.listenerdraw

import android.view.MotionEvent
import com.skin.rbx.clothes.makek.core.custom.drawview.DrawView


interface DrawEvent {
    fun onActionDown(tattooView: DrawView?, event: MotionEvent?)
    fun onActionMove(tattooView: DrawView?, event: MotionEvent?)
    fun onActionUp(tattooView: DrawView?, event: MotionEvent?)
}