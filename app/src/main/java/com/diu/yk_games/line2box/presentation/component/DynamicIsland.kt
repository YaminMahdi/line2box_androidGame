package com.diu.yk_games.line2box.presentation.component

import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diu.yk_games.line2box.model.DynamicBubble
import com.diu.yk_games.line2box.model.DynamicBubble.Idle
import com.diu.yk_games.line2box.util.getSystemBars

private val Mint = Color(0xFF8BF3D0)
private val Ink = Color(0xFF07060D)

/**
 * Wires the island into an XML layout.
 *
 * @param sourceView the view the glass refracts — your FragmentContainerView.
 *        Must NOT be an ancestor of this ComposeView.
 */
fun ComposeView.installDynamicIsland(
    sourceView: View,
    blurRadius: Dp = 50.dp,
    topPadding: Dp = getSystemBars().top.dp + 50.dp,
    onClick: ((state: DynamicBubble) -> Unit)
) {
    setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        val bubble by DynamicIslandController.state.collectAsStateWithLifecycle()
        val backdrop = rememberLiquidBackdrop(blurRadius)

        Box(Modifier.fillMaxSize()) {
            // Draws nothing — it only records the fragment content into the backdrop.
            // Capture pauses on Idle: the notch is 34dp tall over the status bar,
            // so a frozen sample there is invisible and costs nothing.
            Box(
                Modifier
                    .fillMaxSize()
                    .viewBackdropSource(
                        backdrop = backdrop,
                        sourceView = sourceView,
                        blurRadius = blurRadius,
                        live = bubble != Idle,
                    )
            )

            DynamicIsland(
                bubble = bubble,
                backdrop = backdrop,
                modifier = Modifier
                    .align(TopCenter)
                    .padding(top = topPadding),
                onClick = {
                    DynamicIslandController.idle()
                    onClick(bubble)
                },
            )
        }
    }
}

/**
 * An iOS-style Dynamic Island that morphs between [DynamicBubble] states.
 *
 * Place it above content marked with `Modifier.liquidBackdropSource(backdrop)`
 * so the glass has something real to refract.
 */
@Composable
fun DynamicIsland(
    bubble: DynamicBubble,
    backdrop: LiquidBackdrop,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)
) {
    val corner by animateDpAsState(
        targetValue = when (bubble) {
            Idle -> 17.dp
            Loading -> 23.dp
            is DynamicBubble.Message -> 27.dp
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "corner",
    )
    val shape = remember(corner) { RoundedCornerShape(corner) }

    // The squish-and-settle that makes the morph feel like liquid rather than a resize.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(bubble) {
        pop.snapTo(0.90f)
        pop.animateTo(1f, spring(dampingRatio = 0.40f, stiffness = 430f))
    }

    val sheen by rememberSheenProgress()
    val elevated = bubble != Idle

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 10.dp,
                    spread = 4.dp,
                    color = Color.Black.copy(alpha = 0.25f),
                    offset = DpOffset(x = 0.dp, y = 6.dp)
                )
            )
            .shadow(
                elevation = if (elevated) 28.dp else 8.dp,
                shape = shape,
                clip = false,
                ambientColor = Ink,
                spotColor = Ink,
            )
            .liquidGlass(backdrop = backdrop, shape = shape, sheen = sheen)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Center,
    ) {
        AnimatedContent(
            targetState = bubble,
            transitionSpec = {
                val enter = fadeIn(tween(200, delayMillis = 70)) +
                    scaleIn(initialScale = 0.84f, animationSpec = tween(280, delayMillis = 40))
                val exit = fadeOut(tween(110)) + scaleOut(targetScale = 0.92f, animationSpec = tween(110))
                enter togetherWith exit using SizeTransform(clip = false) { _, _ ->
                    spring(
                        dampingRatio = 0.78f,
                        stiffness = 300f,
                        visibilityThreshold = IntSize.VisibilityThreshold,
                    )
                }
            },
            contentAlignment = Center,
            label = "islandContent",
        ) { state ->
            when (state) {
                Idle -> Unit
                Loading -> LoadingContent()
                is DynamicBubble.Message -> MessageContent(state)
            }
        }
    }
}

/* ---------------------------------------------------------------- states */

@Composable
private fun LoadingContent() {
    Row(
        modifier = Modifier
            .height(46.dp)
            .defaultMinSize(minWidth = 190.dp)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArcSpinner()
        Spacer(Modifier.width(11.dp))
        Text(
            text = "Working",
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(10.dp))
        PulsingDots()
    }
}

@Composable
private fun MessageContent(message: DynamicBubble.Message) {
    Row(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 320.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.16f)),
            contentAlignment = Center,
        ) {
            Canvas(Modifier.size(13.dp)) {
                val w = size.width
                val h = size.height
                drawLine(
                    color = Mint,
                    start = Offset(w * 0.06f, h * 0.55f),
                    end = Offset(w * 0.38f, h * 0.86f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = Round,
                )
                drawLine(
                    color = Mint,
                    start = Offset(w * 0.38f, h * 0.86f),
                    end = Offset(w * 0.94f, h * 0.16f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = Round,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = message.name,
                color = Mint.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message.text,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ---------------------------------------------------------------- pieces */

@Composable
private fun ArcSpinner(size: Dp = 18.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    val sweep by transition.animateFloat(
        initialValue = 40f,
        targetValue = 260f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "sweep",
    )

    Canvas(Modifier.size(size).rotate(angle)) {
        val stroke = 2.2.dp.toPx()
        val inset = stroke / 2f
        drawArc(
            brush = Brush.sweepGradient(
                listOf(Mint.copy(alpha = 0f), Mint.copy(alpha = 0.35f), Mint)
            ),
            startAngle = 0f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(width = stroke, cap = Round),
        )
    }
}

@Composable
private fun PulsingDots(count: Int = 3) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.55f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(520),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 140),
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(5.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = scale
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            )
        }
    }
}
