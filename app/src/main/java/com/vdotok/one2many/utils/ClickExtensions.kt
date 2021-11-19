package com.vdotok.one2many.utils

import android.app.Activity
import android.net.ConnectivityManager
import android.view.View
import java.util.concurrent.TimeUnit

fun Activity.hasNetworkAvailable(): Boolean {
    val service = Activity.CONNECTIVITY_SERVICE
    val manager = this.getSystemService(service) as ConnectivityManager?
    val network = manager?.activeNetworkInfo
    return (network != null)
}




fun View.disableDoubleClick() {
    isClickable = false
    postDelayed({ isClickable = true }, TimeUnit.SECONDS.toMillis(2))
}
fun View.disableDoubleClickForOneSec() {
    isClickable = false
    postDelayed({ isClickable = true }, TimeUnit.SECONDS.toMillis(1))
}

fun View.clicks(method:  () -> Unit) {
    setOnClickListener {
        method.invoke()
    }
}

fun View.performSingleClick(method: () -> Unit) {
    setOnClickListener {
        disableDoubleClick()
        method.invoke()
    }
}

fun View.performSingleClickShortDelay(method: () -> Unit) {
    setOnClickListener {
        disableDoubleClickForOneSec()
        method.invoke()
    }
}

fun View.enable() {
    isEnabled = true
}

fun View.disable() {
    isEnabled = false
}

