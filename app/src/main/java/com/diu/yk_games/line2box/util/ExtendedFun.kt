@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package com.diu.yk_games.line2box.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**Flow collect from Fragment with `repeatOnLifecycle` on` lifecycleScope` till `RESUMED` */
context(Fragment)
fun <T> Flow<T?>.collectWithLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.(T) -> Unit,
) {
    lifecycleScope.launch(context) {
        repeatOnLifecycle(minActiveState) {
            filterNotNull().collect { value ->
                if (isAdded && lifecycle.currentState.isAtLeast(minActiveState))
                    block(value)
            }
        }
    }
}

/**Flow collect from Fragment with `repeatOnLifecycle` on` lifecycleScope` till `RESUMED` */
context(Fragment)
fun <T> Flow<T?>.collectWithLifecycleStateIn(
    context: CoroutineContext = EmptyCoroutineContext,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.(T) -> Unit,
) {
    lifecycleScope.launch(context) {
        repeatOnLifecycle(minActiveState) {
            filterNotNull().stateIn(this).collect { value ->
                if (isAdded && lifecycle.currentState.isAtLeast(minActiveState))
                    block(value)
            }
        }
    }
}

/**Flow collect from Activity with `repeatOnLifecycle` on` lifecycleScope` till `RESUMED` */
context(LifecycleOwner)
fun <T> Flow<T?>.collectWithLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.(T) -> Unit,
) {
    lifecycleScope.launch(context) {
        repeatOnLifecycle(minActiveState) {
            filterNotNull().collect { value ->
                if (lifecycle.currentState.isAtLeast(minActiveState))
                    block(value)
            }
        }
    }
}

fun Long.toDateTimeOld(): String {
    var date = SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(this)
    val day = SimpleDateFormat("dd", Locale.US).format(System.currentTimeMillis())
    if (day.toInt() == date.split(" ")[0].toInt())
        date = date.split(", ")[1]
    return date.toString()
}

fun Long.toDateTime(): String {
    val zoneId = ZoneId.systemDefault() // Uses the device's local time zone
    val now = ZonedDateTime.now(zoneId) // Get current time once
    val dateTime = Instant.ofEpochMilli(this).atZone(zoneId)

    val format = if (dateTime.dayOfMonth == now.dayOfMonth && dateTime.month == now.month && dateTime.year == now.year)
        "hh:mm a"
    else
        "dd MMM, hh:mm a"

    return dateTime.format(DateTimeFormatter.ofPattern(format, Locale.US))
}

fun Window.hideSystemBars() {
//    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        insetsController?.hide(WindowInsets.Type.statusBars())
//        insetsController?.hide(WindowInsets.Type.navigationBars())
//        insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//    } else {
//        @Suppress("DEPRECATION")
//        setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
//    }
    @Suppress("DEPRECATION")
    setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
}

/**
 * Adds a click listener to a view that simulates a bounce effect on touch.
 *
 * @param onClick An optional lambda function to be invoked when the view is clicked.
 */
@SuppressLint("ClickableViewAccessibility")
fun View.setBounceClickListener(onClick: ((View) -> Unit)? = null) {
    val mainScope = MainScope()
    var delay = 0L
    setOnClickListener {
        mainScope.launch {
            delay(150L)
            onClick?.invoke(it)
        }
    }
    setOnTouchListener { v, event ->
        mainScope.launch {
            if (event.action == MotionEvent.ACTION_DOWN) {
                delay(delay)
                val scaleDownX = ObjectAnimator.ofFloat(v, "scaleX", 0.90f)
                val scaleDownY = ObjectAnimator.ofFloat(v, "scaleY", 0.90f)
                scaleDownX.duration = 140L
                scaleDownY.duration = 140L

                val scaleDown = AnimatorSet()
                scaleDown.play(scaleDownX).with(scaleDownY)
                scaleDown.start()
                delay = 140L
            } else {
                val delayNeeded =
                    if (event.action == MotionEvent.ACTION_UP || !event.isInside(v)) delay else 350L
                delay(delayNeeded)
                delay = 0L
                val scaleDownX2 = ObjectAnimator.ofFloat(v, "scaleX", 1f)
                val scaleDownY2 = ObjectAnimator.ofFloat(v, "scaleY", 1f)
                scaleDownX2.duration = 120L
                scaleDownY2.duration = 120L

                val scaleDown2 = AnimatorSet()
                scaleDown2.play(scaleDownX2).with(scaleDownY2)
                scaleDown2.start()
            }
        }
        false
    }
}

/**
 * Checks if a touch event falls within the bounds of a view.
 *
 * @param view The view to check against.
 * @return True if the touch event is inside the view's bounds, false otherwise.
 */
fun MotionEvent.isInside(view: View): Boolean {
    if (view.width == 0 || view.height == 0) return false
    return try {
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val viewMaxX = viewLocation[0] + view.width - 1
        val viewMaxY = viewLocation[1] + view.height - 1
        (rawX <= viewMaxX && rawX >= viewLocation[0] && rawY <= viewMaxY && rawY >= viewLocation[1])
    } catch (_: Exception) {
        false
    }
}

//@SuppressLint("DiscouragedApi")
//fun getNavigationBarHeight(): Int {
//    val resources = this.resources
//
//    val resName =
//        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
//            "navigation_bar_height"
//        else
//        "navigation_bar_height_landscape"
//
//    val id: Int = resources.getIdentifier(resName, "dimen", "android")
//
//    return if (id > 0) {
//        resources.getDimensionPixelSize(id)
//    } else {
//        0
//    }
//}

fun Context?.getClipBoardData(): String {
    this ?: return ""
    val clipBoardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var data = ""
    if (clipBoardManager.primaryClip?.description?.hasMimeType("text/*") == true) {
        clipBoardManager.primaryClip?.itemCount?.let {
            for (i in 0 until it) {
                data += clipBoardManager.primaryClip?.getItemAt(i)?.text ?: ""
            }
        }
    }
    data.log("getClipBoardData")
    return data
}

fun Context?.setClipBoardData(data: String?, toastData: String? = null) {
    this ?: return
    data?.let {
        val clipBoardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(data, data)
        clipBoardManager.setPrimaryClip(clip)
        toast(toastData ?: "Copied to clipboard")
    }
}

fun Any?.log(tag: String = "TAG"): Any? {
    if (this is Throwable)
        Log.e("log> '$tag'", "$tag - $message", this)
    else
        Log.i(
            "log> '$tag'",
            "$tag - $this : ${this?.javaClass?.name?.split('.')?.lastOrNull() ?: ""}"
        )
    return this
}

fun Fragment.toast(msg: String?) {
    if (msg.isNullOrEmpty()) return
    context?.let {
        Toast.makeText(it, msg, Toast.LENGTH_SHORT).show()
    }
}

fun Context?.toast(msg: String?) {
    if (msg.isNullOrEmpty()) return
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context?.showCustomTab(url: String?): Unit? {
    if (this == null || url.isNullOrEmpty()) return null
    return try {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(this, url.toUri())
    } catch (_: Exception) {
        null
    }
}

fun Context?.showOnMarket(packageName: String) {
    if (this == null) return
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW).setData("market://details?id=$packageName".toUri()))
    }
}

fun View.show() {
    if (visibility != View.VISIBLE)
        visibility = View.VISIBLE
}

fun View.invisible() {
    if (visibility != View.INVISIBLE)
        visibility = View.INVISIBLE
}

fun View.gone() {
    if (visibility != View.GONE)
        visibility = View.GONE
}

/**Coroutines Extension Function*/
@Suppress("FunctionName")
suspend fun <T, R> T.IO(block: suspend T.() -> R) = withContext(Dispatchers.IO) {
    block()
}

inline fun <T> tryGet(data: () -> T): T? =
    try {
        data()
    } catch (_: Exception) {
        null
    }

fun Activity.closeKeyboard(nextFocus: View? = null) {
    val view = this.currentFocus
    if (view is EditText) {
        val manager =
            this.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(view.windowToken, 0)
        nextFocus?.requestFocus()
    }
}

var systemBarInsets: SystemBarInsets? = null

data class SystemBarInsets(val top: Int = 50, val bottom: Int = 0)

fun getSystemBars(): SystemBarInsets {
    return systemBarInsets ?: SystemBarInsets()
}
fun View.getSystemBarsHeight(): SystemBarInsets {
    val systemBars = ViewCompat.getRootWindowInsets(this)?.getInsets(WindowInsetsCompat.Type.systemBars())
    return SystemBarInsets(systemBars?.top ?: 50, systemBars?.bottom ?: 30)
}


fun FragmentActivity.setNavStatusPadding(vararg layout: ViewGroup, both: Int = 1) {
    lifecycleScope.launch {
        val insets = systemBarInsets ?: suspendCoroutine {
            window.decorView.post {
                systemBarInsets = window.decorView.getSystemBarsHeight()
                it.resume(systemBarInsets!!)

//                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
//                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
////                val bottomIme = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
//
//                    val top = if (systemBars.top == 0) window.decorView.getStatusBarHeight() else systemBars.top
//                    val bottom = systemBars.bottom
//                    if (it.isActive) {
//                        systemBarInsets = SystemBarInsets(top, bottom)
//                        it.resume(systemBarInsets!!)
//                    }
//                    Log.d("TAG", "setNavStatusPadding: top $top, bottom ${systemBars.bottom}")
//                    ViewCompat.setOnApplyWindowInsetsListener(window.decorView, null)
//                    insets
//                }
            }
        }
        Log.d("TAG", "setNavStatusPadding: $systemBarInsets")
        layout.forEach {
            when (both) {
                0 -> it.setPaddingRelative(0, insets.top, 0, 0)
                1 -> it.setPaddingRelative(0, insets.top, 0, insets.bottom)
                -1 -> it.setPadding(0, it.paddingTop, 0, insets.bottom)
            }
        }
    }
}

/**
 * Loads an image into an [ImageView] using Coil.
 *
 * This function supports various data types for loading images, including:
 * - [String] (mapped to a [Uri])
 * - [Uri] ("android.resource", "content", "file", "http", and "https" schemes only)
 * - [File]
 * - [DrawableRes]
 * - [Drawable]
 * - [Bitmap]
 * - [ByteArray]
 * - [ByteBuffer]
 *
 * @receiver The [ImageView] instance.
 * @param data The data to load.
 */
fun ImageView.loadDrawable(data: Any?) {
    val request = ImageRequest.Builder(this.context)
        .crossfade(true)
        .data(data)
        .target(this)
        .build()

    context.imageLoader.enqueue(request)
}

context(Fragment)
fun OnBackPressedCallback.onBackPressedIgnoreCallback() {
    activity?.closeKeyboard()
    isEnabled = false
    activity?.onBackPressedDispatcher?.onBackPressed()
    isEnabled = true
}

context(FragmentActivity)
fun OnBackPressedCallback.onBackPressedIgnoreCallback() {
    closeKeyboard()
    isEnabled = false
    onBackPressedDispatcher.onBackPressed()
    isEnabled = true
}
