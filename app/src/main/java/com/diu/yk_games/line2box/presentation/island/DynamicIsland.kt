package com.diu.yk_games.line2box.presentation.island

import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.TopCenter
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diu.yk_games.line2box.presentation.component.*
import com.diu.yk_games.line2box.util.getSystemBars

private val Mint = Color(0xFF8BF3D0)
private val Ink = Color(0xFF07060D)

fun ComposeView.installDynamicIsland(
    sourceView: View,
    blurRadius: Dp = 50.dp,
    topPadding: Dp = getSystemBars().top.dp + 50.dp,
    spacing: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        val bubbles by DynamicIslandController.state.collectAsStateWithLifecycle()
        val backdrop = rememberLiquidBackdrop(blurRadius)
        val haptics = LocalHapticFeedback.current

        // Buzz once per *new* message, not on every stack mutation.
        val buzzed = remember { mutableSetOf<Long>() }
        LaunchedEffect(bubbles) {
            bubbles.forEach { bubble ->
                if (buzzed.add(bubble.id) && bubble is DynamicBubble.Message)
                    haptics.performHapticFeedback(LongPress)
            }
            buzzed.retainAll(bubbles.mapTo(HashSet()) { it.id })
        }

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .viewBackdropSource(
                        backdrop = backdrop,
                        sourceView = sourceView,
                        blurRadius = blurRadius,
                        live = bubbles.isNotEmpty(),
                    )
            )

            DynamicIslandStack(
                bubbles = bubbles,
                backdrop = backdrop,
                spacing = spacing,
                modifier = Modifier
                    .align(TopCenter)
                    .padding(top = topPadding),
                onClick = { bubble ->
                    if (bubble !is DynamicBubble.Loading)
                        DynamicIslandController.dismiss(bubble.id)
                    (bubble as? DynamicBubble.Message)?.onClick?.invoke()
                }
            )
            content()
        }
    }
}

/* ----------------------------------------------------------------- stack */

/** Keeps a bubble on screen through its exit animation, after the controller drops it. */
private class StackEntry(initial: DynamicBubble) {
    val id = initial.id
    var bubble by mutableStateOf(initial)
    val visible = MutableTransitionState(false).apply { targetState = true }
}

@Composable
fun DynamicIslandStack(
    bubbles: List<DynamicBubble>,
    backdrop: LiquidBackdrop,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    onClick: (DynamicBubble) -> Unit,
) {
    val entries = remember { mutableStateListOf<StackEntry>() }

    LaunchedEffect(bubbles) {
        bubbles.forEach { bubble ->
            val existing = entries.firstOrNull { it.id == bubble.id }
            if (existing == null) entries.add(StackEntry(bubble)) else existing.bubble = bubble
        }
        entries.forEach { entry ->
            if (bubbles.none { it.id == entry.id }) entry.visible.targetState = false
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        // Newest sits under the notch; older ones settle beneath it.
        entries.asReversed().forEachIndexed { depth, entry ->
            key(entry.id) {
                LaunchedEffect(entry.visible.isIdle, entry.visible.currentState) {
                    if (entry.visible.isIdle && !entry.visible.currentState) entries.remove(entry)
                }

                val settle = spring<Float>(dampingRatio = 0.62f, stiffness = 420f)
                AnimatedVisibility(
                    visibleState = entry.visible,
                    enter = fadeIn(tween(180)) +
                            scaleIn(initialScale = 0.86f, animationSpec = settle) +
                            expandVertically(
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                                clip = false,
                            ),
                    exit = fadeOut(tween(140)) +
                            scaleOut(targetScale = 0.88f, animationSpec = tween(140)) +
                            shrinkVertically(tween(170), clip = false),
                ) {
                    val d = depth.coerceAtMost(2)
                    val depthScale by animateFloatAsState(1f - 0.035f * d, label = "depthScale")
                    val depthAlpha by animateFloatAsState(1f - 0.16f * d, label = "depthAlpha")

                    DynamicIsland(
                        bubble = entry.bubble,
                        backdrop = backdrop,
                        modifier = Modifier.graphicsLayer {
                            scaleX = depthScale
                            scaleY = depthScale
                            alpha = depthAlpha
                        },
                        onClick = { onClick(entry.bubble) },
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- island */

@Composable
fun DynamicIsland(
    bubble: DynamicBubble,
    backdrop: LiquidBackdrop,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit),
) {
    val corner by animateDpAsState(
        targetValue = when (bubble) {
            is DynamicBubble.Loading -> 23.dp
            is DynamicBubble.Message -> 27.dp
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "corner",
    )
    val shape = remember(corner) { RoundedCornerShape(corner) }

    // The squish-and-settle that makes a content swap feel like liquid, not a resize.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(bubble) {
        pop.snapTo(0.90f)
        pop.animateTo(1f, spring(dampingRatio = 0.40f, stiffness = 430f))
    }

    val sheen by rememberSheenProgress()

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
                    offset = DpOffset(x = 0.dp, y = 6.dp),
                ),
            )
            .shadow(
                elevation = 28.dp,
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
                val exit = fadeOut(tween(110)) +
                        scaleOut(targetScale = 0.92f, animationSpec = tween(110))
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
                is DynamicBubble.Loading -> LoadingContent(state.label)
                is DynamicBubble.Message -> MessageContent(state)
            }
        }
    }
}

/* ---------------------------------------------------------------- states */

@Composable
private fun LoadingContent(label: String) {
    Row(
        modifier = Modifier
            .height(46.dp)
            .defaultMinSize(minWidth = 170.dp)
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArcSpinner()
        Spacer(Modifier.width(11.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(10.dp))
        PulsingDots()
    }
}


/* ---------------------------------------------------------------- pieces */

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
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "Checkmark",
                modifier = Modifier.size(20.dp),
                tint = Mint
            )
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
                colorStops = arrayOf(
                    0.0f to Mint.copy(alpha = 0f),
                    0.1f to Mint.copy(alpha = 0.2f),
                    0.5f to Mint.copy(alpha = 0.35f),
                    1.0f to Mint
                )
            ),
            startAngle = 0f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(this.size.width - stroke, this.size.height - stroke),
            style = Stroke(width = stroke, cap = Butt)
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