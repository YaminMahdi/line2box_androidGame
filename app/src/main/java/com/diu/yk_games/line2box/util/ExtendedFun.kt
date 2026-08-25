@file:Suppress( "unused")

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
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.fragment.findNavController
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.target
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
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
import kotlin.time.Duration.Companion.milliseconds

///**Flow collect from Fragment with `repeatOnLifecycle` on` lifecycleScope` till `RESUMED` */
//context(f: Fragment)
//fun <T> Flow<T?>.collectWithLifecycle(
//    context: CoroutineContext = EmptyCoroutineContext,
//    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
//    block: suspend CoroutineScope.(T) -> Unit,
//) {
//    f.lifecycleScope.launch(context) {
//        f.repeatOnLifecycle(minActiveState) {
//            filterNotNull().collect { value ->
//                if (f.isAdded && f.lifecycle.currentState.isAtLeast(minActiveState))
//                    block(value)
//            }
//        }
//    }
//}

/**Flow collect from Fragment with `repeatOnLifecycle` on` lifecycleScope` till `RESUMED` */
context(f: Fragment)
fun <T> Flow<T?>.collectWithLifecycleStateIn(
    context: CoroutineContext = EmptyCoroutineContext,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    block: suspend CoroutineScope.(T) -> Unit,
) {
    f.lifecycleScope.launch(context) {
        f.repeatOnLifecycle(minActiveState) {
            filterNotNull().stateIn(this).collect { value ->
                if (f.isAdded && f.lifecycle.currentState.isAtLeast(minActiveState))
                    block(value)
            }
        }
    }
}

/**Flow collect from Activity with `repeatOnLifecycle` on` lifecycleScope` till `RESUMED` */
context(l : LifecycleOwner)
fun <T> Flow<T?>.collectWithLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    block: suspend CoroutineScope.(T) -> Unit,
) {
    l.lifecycleScope.launch(context) {
        l.repeatOnLifecycle(minActiveState) {
            filterNotNull().collect { value ->
                if (l.lifecycle.currentState.isAtLeast(minActiveState))
                    block(value)
            }
        }
    }
}

/**Flow collect from Fragment on` lifecycleScope` if `isAdded` and `RESUMED` */
context(l : LifecycleOwner)
fun <T> Flow<T?>.collectWithLifecycleNoRepeat(
    context: CoroutineContext = EmptyCoroutineContext,
    minActiveState: Lifecycle.State = Lifecycle.State.CREATED,
    block: suspend CoroutineScope.(T) -> Unit,
) {
    l.lifecycleScope.launch(context) {
        filterNotNull().collect { value ->
            if (l.lifecycle.currentState.isAtLeast(minActiveState))
                block(value)
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
            delay(150L.milliseconds)
            onClick?.invoke(it)
        }
    }
    setOnTouchListener { v, event ->
        mainScope.launch {
            if (event.action == MotionEvent.ACTION_DOWN) {
                delay(delay.milliseconds)
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
                delay(delayNeeded.milliseconds)
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
    return !(view.width == 0 || view.height == 0) && try {
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

fun <T> T?.getTag(): String = this?.javaClass
    ?.simpleName
    ?.takeIf { it.isNotBlank() }
    ?: "TAG"

fun Any.cat(message: String) {
    message.log(getTag())
}
context(cls: Any)
fun Any?.log(tag: String = cls.getTag()): Any? {
    var finalTag = cls.getTag()
    if (finalTag != tag)
        finalTag = "$finalTag - $tag"

    if (this is Throwable)
        Log.e("log> '$finalTag'", "$finalTag - $message", this)
    else
        Log.i("log> '$finalTag'", "$finalTag - $this : ${getTag()}")
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

fun View.changeVisibility(isVisible: Boolean, useGone: Boolean = true) {
    visibility = if (isVisible) View.VISIBLE else if (useGone) View.GONE else View.INVISIBLE
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

fun Boolean.toYesNo() = if (this) "YES" else "NO"
fun Boolean.toUnitOrNull() = if (this) Unit else null
fun Boolean?.isTrue() = this == true
fun Boolean?.isFalse() = this == null || !this

inline fun Boolean?.isTrue(next: () -> Unit): Boolean? {
    if (isTrue()) next()
    return this
}

inline fun Boolean?.isFalse(next: () -> Unit): Boolean? {
    if (isFalse()) next()
    return this
}

inline fun String?.isNotEmpty(next: (String) -> Unit): String? {
    if (!isNullOrEmpty()) next(this)
    return this
}

inline fun <T> Collection<T>?.isEmpty(next: () -> Unit): Collection<T>? {
    if (isNullOrEmpty()) next()
    return this
}

inline fun <T> Collection<T>?.isNotEmpty(next: (List<T>) -> Unit): Collection<T>? {
    if (!isNullOrEmpty()) next(toList())
    return this
}

fun String?.isBanglaText() = this == null || matches("[\\u0980-\\u09FF\\s,।_/-]+".toRegex())

fun String?.isValidEmail() =
    this == null || isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidPhoneBD(): Boolean {
    val phoneNumberRegexBd = "^(?:\\+?88)?01[3-9]\\d{8}$".toRegex()
    return matches(phoneNumberRegexBd)
}

fun String.isValidPhoneUS(): Boolean {
    val usPhoneNumberRegex = """^(?:\+?1[-. ]?)?\(?([2-9][0-8][0-9])\)?[-. ]?([2-9][0-9]{2})[-. ]?([0-9]{4})$""".toRegex()
    return matches(usPhoneNumberRegex)
}

fun String.isValidPassword(): Boolean {
    val hasUpper = any { it.isUpperCase() }
    val hasLower = any { it.isLowerCase() }
    val hasNumber = any { it.isDigit() }
    val isGraterThan8 = length >= 8
//    val hasSpecial = this.any { "!@#\$%^&*()-_=+[]{};:'\",.<>?/\\|`~".contains(it) }
    return hasUpper && hasLower && hasNumber && isGraterThan8
}

fun String?.isJson(): Boolean {
    if (this == null) return false
    // A regex pattern for valid JSON
    val jsonPattern = """^\s*(\{.*\}|\[.*])\s*$""".toRegex()
    // Check if the string matches the JSON pattern
    return jsonPattern.matches(this)
}

fun String.isValidURL(): Boolean {
    val urlRegex = Regex("^(https?|ftp)://[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?$", RegexOption.IGNORE_CASE)
    return urlRegex.matches(this)
}


fun <T> T.isNull() = this == null
fun <T> T.isNotNull() = this != null

inline fun <T> T?.isNull(next: () -> Unit): T? {
    if (this == null) next()
    return this
}

inline fun <T> T?.isNotNull(next: (T) -> Unit): T? {
    if (this != null) next(this)
    return this
}

fun Int?.isNullOrZero() = this == null || this == 0
fun Double?.toOneIfZero() = if (this == 0.0 || this == null) 1.0 else this
fun String?.toNAifEmpty() =
    if (isNullOrEmpty() || isBlank() || (this == "0" || this == "0.0")) "N/A" else this

fun String?.toNullifEmpty() =
    if (isNullOrEmpty() || isBlank() || (this == "0" || this == "0.0")) null else this

fun String?.toDoubleOrZero() =
    this?.replace("[^\\d.]".toRegex(), "")?.toDoubleOrNull().orZero().roundTo(2)

fun Int?.orMinusOne() = this ?: -1
fun Long?.orMinusOne() = this ?: -1L
fun Int?.orZero() = this ?: 0
fun Long?.orZero() = this ?: 0L
fun Double?.orZero() = this ?: 0.0
fun Float?.orZero() = this ?: 0.0F
fun String?.orZero() = this?.toIntOrNull() ?: 0
fun String?.orZeroD() = this?.toDoubleOrNull() ?: 0.0
fun Boolean?.orFalse() = this ?: false
fun Boolean.orNull() = if(!this) null else true

fun Int?.isZero() = this == null || this == 0
fun Int?.isMinusOne() = this == null || this == -1
fun Long?.isZero() = this == null || this == 0L
fun Double?.isZero() = this == null || this == 0.0
fun Float?.isZero() = this == null || this == 0.0F
fun Float?.isMinusOne() = this == null || this == -1.0F
fun Float?.isZeroOrMinusOne() = this == null || this == 0.0F || this == -1.0F
fun String?.isZero() = this == null || this.toIntOrNull() == 0
fun String?.isZeroD() = this == null || this.toDoubleOrNull() == 0.0

fun Number.roundTo(numFractionDigits: Int = 2): Double =
    "%.${numFractionDigits}f".format(toDouble(), Locale.ENGLISH).toDouble()

inline fun <T> tryGet(data: () -> T): T? =
    try {
        data()
    } catch (_: Exception) {
        null
    }

// 1. Core View Extensions
fun View.closeKeyboard(window: Window) {
    WindowCompat
        .getInsetsController(window, this)
        .hide(WindowInsetsCompat.Type.ime())
    if (hasFocus()) clearFocus()
}

fun View.showKeyboard(window: Window) {
    requestFocus()
    post {
        WindowCompat
            .getInsetsController(window, this)
            .show(WindowInsetsCompat.Type.ime())
    }
}

// 2. Activity Extensions
fun Activity.closeKeyboard() {
    val view = currentFocus ?: window.decorView
    view.closeKeyboard(window)
}

fun Activity.showKeyboard(targetView: View) {
    targetView.showKeyboard(window)
}

// 3. Fragment Extensions
fun Fragment.closeKeyboard() {
    val currentWindow = activity?.window ?: return
    val targetView = view?.findFocus() ?: view ?: currentWindow.decorView
    targetView.closeKeyboard(currentWindow)
}

context(f: Fragment)
fun View.showKeyboard() {
    f.activity?.showKeyboard(this)
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

fun FragmentActivity.isKeyboardOpen(): Boolean {
    val insets = ViewCompat.getRootWindowInsets(window.decorView)
    return insets?.isVisible(WindowInsetsCompat.Type.ime()) == true
}

fun FragmentActivity.setNavStatusPadding(vararg layout: ViewGroup, both: Int = 1) {
    lifecycleScope.launch {
        val insets = systemBarInsets ?: suspendCancellableCoroutine {
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//                val bottomIme = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

                val top = if (systemBars.top == 0) window.decorView.getSystemBarsHeight().top else systemBars.top
                val bottom = systemBars.bottom
                if (it.isActive) {
                    systemBarInsets = SystemBarInsets(top, bottom)
                    it.resume(systemBarInsets!!)
                }
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView, null)
                Log.d("TAG", "setNavStatusPadding: top $top, bottom ${systemBars.bottom}")
                insets
            }
        }
        Log.d("TAG", "setNavStatusPadding: $insets")
        layout.forEach {
            when (both) {
                0 -> it.updatePadding(top = insets.top)
                1 -> it.updatePadding(top = insets.top, bottom = insets.bottom)
                -1 -> it.updatePadding(bottom = insets.bottom)
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


fun <T: Any> Fragment.navigateSafe(
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {}
): Unit? {
    return try {
        closeKeyboard()
        findNavController().navigate(route) {
            builder()
            launchSingleTop = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
fun Fragment?.navigateUpSafe(): Unit? {
    if(this == null) return null
    return tryGet {
        closeKeyboard()
        findNavController().navigateUp().toUnitOrNull()
    }
}
fun Fragment?.popBackSafe(): Unit? {
    if(this == null) return null
    return tryGet {
        closeKeyboard()
        findNavController().popBackStack().toUnitOrNull()
    }
}

context(f: Fragment)
fun OnBackPressedCallback.onBackPressedIgnoreCallback() {
    f.activity?.closeKeyboard()
    isEnabled = false
    f.activity?.onBackPressedDispatcher?.onBackPressed()
    isEnabled = true
}

context(f: FragmentActivity)
fun OnBackPressedCallback.onBackPressedIgnoreCallback() {
    f.closeKeyboard()
    isEnabled = false
    f.onBackPressedDispatcher.onBackPressed()
    isEnabled = true
}

fun Fragment.onBackPressed(view: View? = null) {
    val activity = activity ?: return
    view?.closeKeyboard(activity.window) ?: closeKeyboard()
    activity.onBackPressedDispatcher.onBackPressed()
}

fun FragmentActivity.onBackPressed(view: View? = null) {
    view?.closeKeyboard(window) ?: closeKeyboard()
    onBackPressedDispatcher.onBackPressed()
}

fun ImageView.setDrawableWithFade(@DrawableRes background: Int, durationMs: Int = 500) {
    val newDrawable = ContextCompat.getDrawable(context, background) ?: return
    val currentBackground = (drawable as? TransitionDrawable)
        ?.getDrawable(1)
        ?: drawable
        ?: 0.toDrawable()

    if (newDrawable.constantState == currentBackground.constantState) return

    val transitionDrawable = TransitionDrawable(arrayOf(currentBackground, newDrawable))
    setImageDrawable(transitionDrawable)
    transitionDrawable.startTransition(durationMs)
    transitionDrawable.isCrossFadeEnabled = true
}
