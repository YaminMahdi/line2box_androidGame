package com.diu.yk_games.line2box.util

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import com.diu.yk_games.line2box.BuildConfig
import com.diu.yk_games.line2box.databinding.FragmentGameDualBinding

class LineSelectorUtil(
    private val binding: FragmentGameDualBinding
) {
    val lineSelector: View get() = binding.lineSelector
    val parentLayout: ViewGroup get() = binding.relativeLayout

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
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else if (animated) {
            selector.animate()
                .translationX(position.translationX)
                .translationY(position.translationY)
                .rotation(position.rotation)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(250L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        } else {
            selector.translationX = position.translationX
            selector.translationY = position.translationY
            selector.rotation = position.rotation
            selector.alpha = 1f
            selector.scaleX = 1f
            selector.scaleY = 1f
        }
    }

    fun moveToLine(idNm: String, targetView: View? = null, animated: Boolean = true) =
        moveSelector(idNm, targetView, animated)

    fun move(idNm: String, targetView: View? = null, animated: Boolean = true) =
        moveSelector(idNm, targetView, animated)

    fun reset() {
        lineSelector.visibility = View.INVISIBLE
        lineSelector.animate().cancel()
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
}