package com.campusconnect.app.utils

import android.view.View
import android.view.animation.AnimationUtils
import com.campusconnect.app.R

fun View.setBlinkOnClick(onClick: () -> Unit) {
    this.setOnClickListener {
        val animation = AnimationUtils.loadAnimation(context, R.anim.blink)
        this.startAnimation(animation)
        onClick()
    }
}