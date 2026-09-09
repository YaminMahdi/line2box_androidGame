package com.diu.yk_games.line2box.util

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding

class LineSelectorUtil(
    private val binding: FragmentGameDualBinding
) {
    val lineSelector: View get() = binding.lineSelector
    val parentLayout: ViewGroup get() = binding.relativeLayout

    private var pulseAnimator: ObjectAnimator? = null

    init {
        lineSelector.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) = stopPulseAnimation()
        })
    }

    data class SelectorPosition(
        val translationX: Float,
        val translationY: Float,
        val rotation: Float
    )

    /**
     * Calculates the required translationX, translationY, and rotation to position
     * [lineSelector] over the line identified by [idNm].
     */
    fun calculatePosition(idNm: String, targetView: View? = null): SelectorPosition? {
        val view = targetView ?: findTargetView(idNm) ?: return null
        val selector = lineSelector
        val parent = parentLayout

        if (selector.width == 0 || view.width == 0) return null

        val targetRect = Rect()
        view.getDrawingRect(targetRect)
        try {
            parent.offsetDescendantRectToMyCoords(view, targetRect)
        } catch (_: Exception) {
            val parentView = view.parent as? View
            val x = view.left + (parentView?.left ?: 0)
            val y = view.top + (parentView?.top ?: 0)
            targetRect.set(x, y, x + view.width, y + view.height)
        }

        val targetCenterX = targetRect.exactCenterX()
        val targetCenterY = targetRect.exactCenterY()

        val selectorBaseCenterX = selector.left + selector.width / 2f
        val selectorBaseCenterY = selector.top + selector.height / 2f

        val targetTranslationX = targetCenterX - selectorBaseCenterX
        val targetTranslationY = targetCenterY - selectorBaseCenterY
        val targetRotation = if (idNm.endsWith("L", ignoreCase = true)) 90f else 0f

        return SelectorPosition(
            translationX = targetTranslationX,
            translationY = targetTranslationY,
            rotation = targetRotation
        )
    }

    /**
     * Starts the idle pulsing animation matching the breathing animation in TurnBattleBar.
     * Scales between [PULSE_SCALE_MIN] and [PULSE_SCALE_MAX] with a duration of [PULSE_DURATION_MS]
     * using [FastOutSlowInInterpolator] and reverse repeat mode.
     */
    fun startPulseAnimation() {
        val selector = lineSelector
        if (!selector.isVisible) return
        if (pulseAnimator?.isRunning == true) return
        pulseAnimator?.cancel()

        selector.pivotX = selector.width / 2f
        selector.pivotY = selector.height / 2f

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, PULSE_SCALE_MIN, PULSE_SCALE_MAX)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, PULSE_SCALE_MIN, PULSE_SCALE_MAX)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(selector, scaleX, scaleY).apply {
            duration = PULSE_DURATION_MS
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    /**
     * Stops the idle pulsing animation.
     */
    fun stopPulseAnimation(resetScale: Boolean = false) {
        pulseAnimator?.cancel()
        pulseAnimator = null
        if (resetScale) {
            lineSelector.scaleX = 1f
            lineSelector.scaleY = 1f
        }
    }

    /**
     * Moves [lineSelector] to the target line identified by [idNm] with smooth animation.
     */
    fun moveSelector(idNm: String, targetView: View? = null, animated: Boolean = true) {
        val view = targetView ?: findTargetView(idNm) ?: return
        val selector = lineSelector

        // Ensure views are laid out before calculating positions
        if (!selector.isLaidOut || !view.isLaidOut || selector.width == 0 || view.width == 0) {
            selector.post {
                moveSelector(idNm, view, animated)
            }
            return
        }

        val position = calculatePosition(idNm, view) ?: return

        selector.pivotX = selector.width / 2f
        selector.pivotY = selector.height / 2f

        if (!selector.isVisible) {
            // First time showing: snap to position and fade/scale in
            selector.translationX = position.translationX
            selector.translationY = position.translationY
            selector.rotation = position.rotation
            selector.alpha = 0f
            selector.scaleX = 0.5f
            selector.scaleY = 0.5f
            selector.visibility = View.VISIBLE
            selector.animate()
                .alpha(1f)
                .scaleX(PULSE_SCALE_MIN)
                .scaleY(PULSE_SCALE_MIN)
                .setDuration(200L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    if (selector.isVisible) {
                        startPulseAnimation()
                    }
                }
                .start()
        } else if (animated) {
            if (pulseAnimator?.isRunning != true) {
                selector.animate()
                    .translationX(position.translationX)
                    .translationY(position.translationY)
                    .rotation(position.rotation)
                    .scaleX(PULSE_SCALE_MIN)
                    .scaleY(PULSE_SCALE_MIN)
                    .alpha(1f)
                    .setDuration(250L)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        if (selector.isVisible) {
                            startPulseAnimation()
                        }
                    }
                    .start()
            } else {
                selector.animate()
                    .translationX(position.translationX)
                    .translationY(position.translationY)
                    .rotation(position.rotation)
                    .alpha(1f)
                    .setDuration(250L)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        } else {
            selector.animate().cancel()
            selector.translationX = position.translationX
            selector.translationY = position.translationY
            selector.rotation = position.rotation
            selector.alpha = 1f
            if (pulseAnimator?.isRunning != true) {
                selector.scaleX = PULSE_SCALE_MIN
                selector.scaleY = PULSE_SCALE_MIN
                startPulseAnimation()
            }
        }
    }

    fun reset() {
        stopPulseAnimation(resetScale = true)
        lineSelector.animate().cancel()
        lineSelector.visibility = View.INVISIBLE
        lineSelector.translationX = 0f
        lineSelector.translationY = 0f
        lineSelector.rotation = 0f
    }

    @SuppressLint("DiscouragedApi")
    private fun findTargetView(idNm: String): View? {
        val resId = binding.root.context.resources.getIdentifier(
            idNm,
            "id",
            BuildConfig.APPLICATION_ID
        )
        return if (resId != 0) binding.root.findViewById(resId) else null
    }

    companion object {
        const val PULSE_SCALE_MIN = 0.90f
        const val PULSE_SCALE_MAX = 1.1f
        const val PULSE_DURATION_MS = 1400L
    }
}