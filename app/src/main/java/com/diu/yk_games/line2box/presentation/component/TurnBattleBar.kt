package com.diu.yk_games.line2box.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diu.yk_games.line2box.R
import com.diu.yk_games.line2box.ui.theme.Line2BoxTheme
import kotlin.math.abs
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.lerp as lerpDp

private val RedTeam = Color(0xFFFF2D55)
private val BlueTeam = Color(0xFF2F86FF)

private val Brad = FontFamily(Font(R.font.brad))
private val Sortie = FontFamily(Font(R.font.sortie))

@Composable
fun TurnBattleBar(
    redName: String,
    blueName: String,
    showTurnText: (forRed: Boolean) -> Boolean,
    isRedTurn: Boolean,
    modifier: Modifier = Modifier,
) {
    // 0f = reds turn, 1f = blue's turn. One spring drives position, color and weighting.
    val progress by animateFloatAsState(
        targetValue = if (isRedTurn) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow),
        label = "turnProgress",
    )
    val teamColor = lerpColor(RedTeam.copy(.5f), BlueTeam.copy(.5f), progress)

    val haptics = LocalHapticFeedback.current
    val vsPop = remember { Animatable(1f) }
    LaunchedEffect(isRedTurn) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vsPop.snapTo(1.35f)
        vsPop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
    }

    val infinite = rememberInfiniteTransition(label = "idle")
    val breathe by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathe",
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "spin",
    )
    val flow by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "flow",
    )

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(100.dp)
    ) {
        val columnWidth = maxWidth * 0.40f
        val slideX = lerpDp(0.dp, maxWidth - columnWidth, progress)

        // Sliding highlight capsule behind the active player.
        Box(
            Modifier
                .offset(x = slideX)
                .width(columnWidth)
                .fillMaxHeight()
                .drawBehind {
                    drawRoundRectGlow(teamColor, breathe)
                }
                .background(
                    brush = Brush.verticalGradient(
                        listOf(teamColor.copy(alpha = 0.1f), teamColor.copy(alpha = 0.05f)),
                    ),
                    shape = RoundedCornerShape(22.dp),
                )
                .border(
                    width = 3.dp,
                    brush = Brush.verticalGradient(
                        listOf(teamColor.copy(alpha = 0.2f), teamColor.copy(alpha = 0.12f)),
                    ),
                    shape = RoundedCornerShape(22.dp),
                ).innerShadow(
                    shape = RoundedCornerShape(22.dp),
                    shadow = Shadow(
                        radius = 10.dp,
                        spread = 5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                teamColor.copy(.2f),
                                Color.White.copy(.2f)
                            ),
                        )
                    )
                )
        )

        // Chevrons streaming toward whoever holds the turn.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawChevrons(teamColor, progress, flow) }
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerSide(
                team = "RED",
                name = redName,
                showTurnText = showTurnText(true),
                teamColor = RedTeam,
                activeness = 1f - progress,
                breathe = breathe,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
            )

            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                VersusCore(
                    teamColor = teamColor,
                    spin = spin,
                    breathe = breathe,
                    pop = vsPop.value,
                )
            }

            PlayerSide(
                team = "BLUE",
                name = blueName,
                showTurnText = showTurnText(false),
                teamColor = BlueTeam,
                activeness = progress,
                breathe = breathe,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun PlayerSide(
    team: String,
    name: String,
    showTurnText: Boolean,
    teamColor: Color,
    activeness: Float,
    breathe: Float,
    modifier: Modifier = Modifier
) {
    val scale = (0.84f + 0.16f * activeness) *
            (1f + (breathe - 1f) * activeness)

    val alpha = 0.34f + 0.66f * activeness

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Text(
            text = team,
            style = TextStyle(
                fontFamily = Brad,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White,
                        lerpColor(
                            Color.White,
                            teamColor.copy(.2f),
                            0.35f + 0.5f * activeness
                        )
                    ),
                )
            ),
            modifier = Modifier
                .dropShadow(
                    shape = RoundedCornerShape(5.dp),
                    shadow = Shadow(
                        radius = 10.dp,
                        spread = 5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                lerpColor(
                                    Color.White.copy(.2f),
                                    teamColor.copy(.2f),
                                    0.35f + 0.5f * activeness
                                ),
                                Color.White.copy(.2f)
                            ),
                        )
                    )
                )
        )

        Text(
            text = name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontFamily = Sortie,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.padding(horizontal = 6.dp),
        )

        if (showTurnText) {
            Box(
                Modifier
                    .background(teamColor.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .border(1.dp, teamColor.copy(alpha = 0.8f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (activeness > 0.85f) "YOUR TURN" else "WAIT",
                    style = TextStyle(
                        fontFamily = Sortie,
                        fontSize = 9.sp,
                        color = Color.White,
                        letterSpacing = 1.6.sp,
                    ),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun VersusCore(
    teamColor: Color,
    spin: Float,
    breathe: Float,
    pop: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val c = center
                val r = size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(teamColor.copy(alpha = 0.5f), Color.Transparent),
                        center = c,
                        radius = r * 1.15f * breathe,
                    ),
                    radius = r * 1.15f * breathe,
                    center = c,
                )

                rotate(spin, c) {
                    repeat(4) { i ->
                        drawArc(
                            color = teamColor.copy(alpha = 0.8f),
                            startAngle = i * 90f + 10f,
                            sweepAngle = 58f,
                            useCenter = false,
                            topLeft = Offset(c.x - r * 0.88f, c.y - r * 0.88f),
                            size = Size(r * 1.76f, r * 1.76f),
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round),
                        )
                    }
                }

                rotate(-spin * 1.7f, c) {
                    repeat(3) { i ->
                        drawArc(
                            color = Color.White.copy(alpha = 0.32f),
                            startAngle = i * 120f + 24f,
                            sweepAngle = 44f,
                            useCenter = false,
                            topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f),
                            size = Size(r * 1.24f, r * 1.24f),
                            style = Stroke(width = 2f, cap = StrokeCap.Round),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "VS",
            modifier = Modifier.graphicsLayer {
                scaleX = pop
                scaleY = pop
                rotationZ = -8f
            },
            style = TextStyle(
                fontFamily = Sortie,
                fontSize = 30.sp,
                lineHeight = 30.sp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White,
                        lerpColor(Color.White, teamColor.copy(.1f), 0.35f + 0.5f)
                    )
                ),
                shadow = Shadow(color = teamColor, blurRadius = 24f),
            ),
        )
    }
}

private fun DrawScope.drawRoundRectGlow(teamColor: Color, breathe: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(teamColor.copy(alpha = 0.22f), Color.Transparent),
            center = center,
            radius = size.minDimension * breathe,
        ),
        radius = size.minDimension * breathe,
        center = center,
    )
}

private fun DrawScope.drawChevrons(teamColor: Color, progress: Float, flow: Float) {
    val dir = (progress - 0.5f) * 2f          // -1 toward red, +1 toward blue
    if (abs(dir) < 0.05f) return

    val originX = size.width / 2f
    val midY = size.height / 2f
    val w = size.width * 0.016f
    val h = size.height * 0.09f

    repeat(3) { i ->
        val t = (flow + i / 3f) % 1f
        val x = originX + dir * (size.width * 0.11f + t * size.width * 0.20f)
        val alpha = (1f - t) * 0.5f * abs(dir)
        val path = Path().apply {
            moveTo(x - dir * w, midY - h)
            lineTo(x + dir * w, midY)
            lineTo(x - dir * w, midY + h)
        }
        drawPath(
            path = path,
            color = teamColor.copy(alpha = alpha),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Preview
@Composable
private fun TurnBattleBarPrev() {
    Line2BoxTheme {
        TurnBattleBar(
            redName = "Red Team",
            blueName = "Blue Team",
            showTurnText = { true },
            isRedTurn = true,
        )
    }
}