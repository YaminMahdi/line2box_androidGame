package com.diu.yk_games.line2box.presentation.component

import android.os.Build
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Liquid Glass for Jetpack Compose — no third-party libraries.
 *
 * How it works:
 *   1. The screen content behind the glass records itself into a GraphicsLayer
 *      (`Modifier.liquidBackdropSource`). A second layer replays it with a
 *      BlurEffect attached, so the blur costs one composite, not a second draw pass.
 *   2. Any glass surface (`Modifier.liquidGlass`) redraws that blurred layer,
 *      translated into its own coordinate space, scaled slightly for a lens/refraction
 *      feel, and clipped to its shape.
 *   3. On top of the sample: a tint, a top inner glow, a traveling specular sheen,
 *      and a directional rim stroke — the four things that sell "glass" more than blur does.
 *
 * Requirements: Compose UI 1.7+ (GraphicsLayer API). Backdrop blur needs Android 12
 * (API 31); below that `liquidGlass` falls back to a smoked-glass scrim automatically.
 *
 * If `record` / `drawLayer` show as unresolved, let the IDE auto-import them — they
 * moved packages between Compose versions.
 */

@Stable
internal val GlassBlurSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Shared handle between the content being sampled and every glass surface above it. */
@Stable
class LiquidBackdrop internal constructor(
    internal val source: GraphicsLayer,
    internal val blurred: GraphicsLayer,
) {
    /** Where the sampled content sits in window coordinates. */
    internal var originInWindow: Offset by mutableStateOf(Offset.Zero)

    /**
     * Bumped every time the backdrop redraws. Glass surfaces read it so they
     * re-sample in lockstep with animated content behind them — and stop
     * redrawing entirely once the backdrop goes still.
     */
    internal var frame: Int by mutableIntStateOf(0)
}

@Composable
fun rememberLiquidBackdrop(blurRadius: Dp = 32.dp): LiquidBackdrop {
    val source = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val backdrop = remember(source, blurred) { LiquidBackdrop(source, blurred) }

    val radiusPx = with(LocalDensity.current) { blurRadius.toPx() }
    remember(radiusPx) {
        blurred.renderEffect =
            if (GlassBlurSupported && radiusPx > 0f) {
                BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
            } else {
                null
            }
        radiusPx
    }
    return backdrop
}

/**
 * Marks the content that glass surfaces sample. Put this on the layer *behind*
 * the island — usually the whole screen body.
 */
fun Modifier.liquidBackdropSource(backdrop: LiquidBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.originInWindow = it.positionInWindow() }
    .drawWithContent {
        backdrop.source.record { this@drawWithContent.drawContent() }
        // Keep the replay layer populated on every API level. Android 12+ applies
        // the blur; older releases still get a refracted, tinted snapshot.
        backdrop.blurred.record { drawLayer(backdrop.source) }
        drawLayer(backdrop.source)
        backdrop.frame++
    }

/**
 * @param refraction how much the sampled backdrop is magnified under the glass.
 *        1f is flat; ~1.08f reads like a thin lens.
 * @param sheen 0f-1f progress of the traveling highlight; pass a negative value to disable.
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: LiquidBackdrop,
    shape: Shape,
    tint: Color = Color(0xFF0C0B14).copy(alpha = 0.42f),
    refraction: Float = 1.035f,
    sheen: Float = -1f,
    rimAlpha: Float = 0.9f,
): Modifier {
    var originInWindow by remember { mutableStateOf(Offset.Zero) }

    return this
        .onGloballyPositioned { originInWindow = it.positionInWindow() }
        .drawWithCache {
            val path = shape.createOutline(size, layoutDirection, this).toPath()

            // Light reads as coming from the top-left: bright at the near edge,
            // a second weaker catch on the far edge, dark along the sides.
            val rim = Brush.linearGradient(
                0.00f to Color.White.copy(alpha = 0.85f * rimAlpha),
                0.28f to Color.White.copy(alpha = 0.12f * rimAlpha),
                0.62f to Color.White.copy(alpha = 0.04f * rimAlpha),
                1.00f to Color.White.copy(alpha = 0.50f * rimAlpha),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
            val innerGlow = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.13f),
                1f to Color.Transparent,
                startY = 0f,
                endY = size.height * 0.7f,
            )
            val rimStroke = Stroke(width = .5.dp.toPx())

            onDrawBehind {
                backdrop.frame // read → re-sample whenever the backdrop moves

                if (backdrop.frame > 0) {
                    val delta = backdrop.originInWindow - originInWindow
                    clipPath(path) {
                        scale(refraction, refraction, pivot = center) {
                            translate(delta.x, delta.y) { drawLayer(backdrop.blurred) }
                        }
                    }
                } else {
                    drawPath(path, Color(0xFF0C0B14).copy(alpha = 0.86f))
                }

                drawPath(path, tint)
                drawPath(path, innerGlow)

                if (sheen >= 0f) {
                    val travel = size.width * 2.2f
                    val x = -size.width * 0.6f + sheen * travel
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            0.00f to Color.Transparent,
                            0.46f to Color.White.copy(alpha = 0.16f),
                            0.54f to Color.White.copy(alpha = 0.16f),
                            1.00f to Color.Transparent,
                            start = Offset(x, 0f),
                            end = Offset(x + size.width * 0.5f, size.height),
                        ),
                    )
                }

                drawPath(path, rim, style = rimStroke)
            }
        }
}

/** Drives the specular sweep. Share one instance across sibling glass surfaces. */
@Composable
fun rememberSheenProgress(durationMillis: Int = 4200): State<Float> {
    val transition = rememberInfiniteTransition(label = "sheen")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheenProgress",
    )
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/*
 * View-sourced Liquid Glass, for XML + Compose hybrids.
 *
 * `liquidBackdropSource` records Compose content. This one records a real View —
 * your FragmentContainerView — by calling View.draw() into the backdrop layer once
 * per frame. Everything downstream (`Modifier.liquidGlass`, DynamicIsland) is unchanged.
 *
 * Known limits, in order of how likely they are to bite:
 *
 *   1. Never pass a view that contains the ComposeView. It would capture the island
 *      drawing itself and smear across frames. Pass the nav host, not the root.
 *   2. SurfaceView / TextureView / VideoView / MapView / hardware WebView do not render
 *      into View.draw() — they live on separate surfaces. Glass over those shows a hole.
 *      For those screens use a PixelCopy snapshot instead, or drop to a plain scrim.
 *   3. View.draw() re-runs the whole subtree's draw each captured frame. Gate it with
 *      `live` so you only pay while the island is actually open.
 */

/*
 * Replaces the earlier ViewBackdrop.kt. Two changes:
 *
 *   1. One layer, not two. The View is already on screen drawing itself, so there is
 *      no use for a sharp copy - we record straight into the blurred layer. This also
 *      removes the nested `record { drawLayer(source) }`, which is the step most likely
 *      to quietly drop the render effect.
 *   2. renderEffect is asserted inside the draw pass. Setting it once during composition
 *      is fragile: if the layer gets released and reissued, the effect goes with it, and
 *      you get a sharp copy that looks exactly like no glass at all.
 *
 * If blur is still missing after this, it is not this code - RenderEffect is a no-op on
 * a software canvas. Check: API 31+, hardwareAccelerated not disabled in the manifest,
 * running on a device or a hardware-GPU emulator, and not looking at @Preview.
 */

@Composable
fun Modifier.viewBackdropSource(
    backdrop: LiquidBackdrop,
    sourceView: View?,
    blurRadius: Dp = 30.dp,
    live: Boolean = true,
): Modifier {
    val composeView = LocalView.current
    val tick = rememberFrameTicker(enabled = live && sourceView != null)
    val location = remember2()

    return this
        .onGloballyPositioned { backdrop.originInWindow = it.positionInWindow() }
        .drawWithContent {
            drawContent()

            val src = sourceView ?: return@drawWithContent
            if (src === composeView || src.contains(composeView)) return@drawWithContent
            tick.value // read -> redraw every frame while live

            val radiusPx = blurRadius.toPx()
            backdrop.blurred.renderEffect =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radiusPx > 0f) {
                    BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
                } else {
                    null
                }

            src.getLocationInWindow(location)
            val dx = location[0] - backdrop.originInWindow.x
            val dy = location[1] - backdrop.originInWindow.y

            backdrop.blurred.record {
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val save = native.save()
                    native.translate(dx, dy)
                    src.draw(native)
                    native.restoreToCount(save)
                }
            }
            backdrop.frame++
        }
}

@Composable
private fun rememberFrameTicker(enabled: Boolean): State<Long> =
    produceState(initialValue = 0L, enabled) {
        if (!enabled) return@produceState
        while (true) {
            withFrameNanos { value = it }
        }
    }

@Composable
private fun remember2(): IntArray =
    remember { IntArray(2) }

/** True if [child] sits anywhere under this view — the feedback-loop guard. */
private fun View.contains(child: View): Boolean {
    var node: View? = child
    while (node != null) {
        if (node === this) return true
        node = node.parent as? View
    }
    return false
}
