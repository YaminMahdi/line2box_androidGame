package com.diu.yk_games.line2box.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*


fun Long.toDateTime(): String{
    var date = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(this)
    val day = SimpleDateFormat("dd", Locale.US).format(System.currentTimeMillis())
    if (day.toInt() == date.split(" ")[0].toInt())
        date = date.split(", ")[1]
    return date.toString()
}

@Suppress("DEPRECATION")
fun Window.hideSystemBars() {
//    WindowInsetsControllerCompat(window, window.decorView).let { controller ->
//        controller.hide(WindowInsetsCompat.Type.systemBars())
//        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//    }
    setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
}
@SuppressLint("ClickableViewAccessibility")
fun View.setBounceClickListener(onClick: ((View) -> Unit)? = null){
    this.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val scaleDownX = ObjectAnimator.ofFloat(
                    v, "scaleX", 0.85f
                )
                val scaleDownY = ObjectAnimator.ofFloat(
                    v, "scaleY", 0.85f
                )
                scaleDownX.duration = 120
                scaleDownY.duration = 120

                val scaleDown = AnimatorSet()
                scaleDown.play(scaleDownX).with(scaleDownY)
                scaleDown.start()

            }
            MotionEvent.ACTION_UP -> {
                val scaleDownX2 = ObjectAnimator.ofFloat(
                    v, "scaleX", 1f
                )
                val scaleDownY2 = ObjectAnimator.ofFloat(
                    v, "scaleY", 1f
                )
                scaleDownX2.duration = 150
                scaleDownY2.duration = 150

                val scaleDown2 = AnimatorSet()
                scaleDown2.play(scaleDownX2).with(scaleDownY2)

                scaleDown2.start()
            }
        }
        false
    }
    this.setOnClickListener { onClick?.invoke(this) }
}

@SuppressLint("DiscouragedApi")
fun Context.getNavigationBarHeight(): Int {
    val resources = this.resources

    val resName =
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            "navigation_bar_height"
        else
        "navigation_bar_height_landscape"

    val id: Int = resources.getIdentifier(resName, "dimen", "android")

    return if (id > 0) {
        resources.getDimensionPixelSize(id)
    } else {
        0
    }
}

fun Context.getClipBoardData() : String {
    val clipBoardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var data=""
    if(clipBoardManager.primaryClip?.description?.hasMimeType("text/*") == true) {
        clipBoardManager.primaryClip?.itemCount?.let {
            for (i in 0 until it) {
                data += clipBoardManager.primaryClip?.getItemAt(i)?.text ?: ""
            }
        }
    }
    return data
}

fun Activity.closeKeyboard(nextFocus: View?= null) {
    val view = this.currentFocus
    if (view is EditText) {
        val manager = this.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(view.getWindowToken(), 0)
        nextFocus?.requestFocus()
    }
}
