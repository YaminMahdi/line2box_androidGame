package com.diu.yk_games.line2box.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.*


fun Long.toDateTime(): String{
    var date = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(this)
    val day = SimpleDateFormat("dd", Locale.US).format(System.currentTimeMillis())
    if (day.toInt() == date.split(" ")[0].toInt())
        date = date.split(", ")[1]
    return date.toString()
}

fun Window.hideSystemBars() {
//    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        insetsController?.hide(WindowInsets.Type.statusBars())
        insetsController?.hide(WindowInsets.Type.navigationBars())
        insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        @Suppress("DEPRECATION")
        setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
//    @Suppress("DEPRECATION")
//    setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
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

fun ViewGroup.setNavStatusPadding(vararg layout: ViewGroup, both: Int = 0){
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val bottomIme = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val top = if(systemBars.top==0) 30 else systemBars.top
        layout.forEach {
            when (both) {
                0 -> it.setPaddingRelative(0, top, 0, 0)
                1 -> it.setPaddingRelative(0, top, 0, if(bottomIme==0) systemBars.bottom else bottomIme)
                -1 -> it.setPadding(0, it.paddingTop, 0,  if(bottomIme==0) systemBars.bottom else bottomIme)
            }
        }
        return@setOnApplyWindowInsetsListener insets
    }
}
