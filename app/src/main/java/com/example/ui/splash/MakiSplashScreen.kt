package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MakiChampagneGold
import com.example.ui.theme.MakiEmerald
import com.example.ui.theme.MakiEmeraldDark
import com.example.ui.theme.MakiEmeraldLight
import com.example.ui.theme.MakiGoldAccent
import com.example.ui.theme.MakiGoldDark
import com.example.ui.theme.MakiGoldLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data model for ambient floating golden particles
 */
private data class GoldenParticle(
    val initialXNorm: Float,
    val initialYNorm: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val phaseOffset: Float,
    val baseAlpha: Float
)

@Composable
fun MakiSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Master timeline progression from 0.0f to 1.0f over ~2.8 seconds
    val timelineProgress = remember { Animatable(0f) }

    // Floating particles continuous ambient wave
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val ambientWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientWave"
    )

    // Run master splash animation sequence
    LaunchedEffect(Unit) {
        timelineProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2800,
                easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
            )
        )
        onSplashFinished()
    }

    val progress = timelineProgress.value

    // --- Timeline Phases ---
    // Phase 1 (0.00 -> 0.35): Entry of 3D Gift Box + Golden Particles
    val entryProgress = (progress / 0.35f).coerceIn(0f, 1f)
    val boxScale = 0.45f + 0.55f * FastOutSlowInEasing.transform(entryProgress)
    val boxAlpha = (progress / 0.22f).coerceIn(0f, 1f)

    // Phase 2 (0.15 -> 0.55): Gentle 3D Yaw & Pitch Rotation
    val rotPhase = ((progress - 0.15f) / 0.40f).coerceIn(0f, 1f)
    val boxRotationY = -16f + 26f * sin(rotPhase * PI.toFloat()).toFloat()
    val boxRotationX = 14f - 8f * rotPhase

    // Phase 3 (0.38 -> 0.70): Ribbon Unties & Lid Lifts in 3D
    val openProgress = ((progress - 0.38f) / 0.32f).coerceIn(0f, 1f)
    val lidLiftNorm = FastOutSlowInEasing.transform(openProgress)

    // Phase 4 (0.48 -> 0.85): Radiant Golden Light Burst from inside the box
    val lightBurstProgress = ((progress - 0.48f) / 0.30f).coerceIn(0f, 1f)
    val lightBurstAlpha = FastOutSlowInEasing.transform(lightBurstProgress)

    // Phase 5 (0.62 -> 0.95): Brand Emblem, Title & Tagline Reveal
    val brandRevealProgress = ((progress - 0.62f) / 0.30f).coerceIn(0f, 1f)
    val brandAlpha = FastOutSlowInEasing.transform(brandRevealProgress)
    val brandScale = 0.85f + 0.15f * brandAlpha

    // Golden particles stable distribution
    val particles = remember {
        listOf(
            GoldenParticle(0.20f, 0.25f, 2.5f, 0.9f, 0.2f, 0.65f),
            GoldenParticle(0.32f, 0.18f, 1.8f, 1.2f, 1.1f, 0.50f),
            GoldenParticle(0.15f, 0.45f, 3.2f, 0.7f, 2.3f, 0.75f),
            GoldenParticle(0.28f, 0.65f, 2.0f, 1.1f, 3.4f, 0.60f),
            GoldenParticle(0.40f, 0.35f, 1.5f, 1.4f, 0.8f, 0.40f),
            GoldenParticle(0.70f, 0.22f, 2.8f, 0.8f, 4.2f, 0.70f),
            GoldenParticle(0.82f, 0.32f, 1.6f, 1.3f, 1.9f, 0.55f),
            GoldenParticle(0.65f, 0.52f, 2.2f, 1.0f, 5.1f, 0.60f),
            GoldenParticle(0.78f, 0.68f, 3.0f, 0.6f, 2.7f, 0.75f),
            GoldenParticle(0.50f, 0.15f, 2.4f, 1.1f, 0.5f, 0.65f),
            GoldenParticle(0.52f, 0.78f, 1.9f, 0.9f, 3.8f, 0.50f),
            GoldenParticle(0.88f, 0.50f, 2.6f, 1.2f, 4.7f, 0.70f),
            GoldenParticle(0.12f, 0.70f, 2.1f, 0.8f, 1.5f, 0.55f),
            GoldenParticle(0.38f, 0.82f, 2.7f, 1.0f, 2.9f, 0.65f),
            GoldenParticle(0.62f, 0.85f, 1.7f, 1.3f, 5.4f, 0.45f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MakiEmeraldLight.copy(alpha = 0.95f),
                        MakiEmerald,
                        MakiEmeraldDark,
                        Color(0xFF031610)
                    ),
                    center = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    radius = 1800f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Allow instant skip on tap for user convenience
                onSplashFinished()
            }
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        // --- 1. AMBIENT GOLD PARTICLES CANVAS ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Center glow spotlight behind the gift box
            val glowRadius = w * (0.45f + 0.10f * sin(ambientWave))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MakiChampagneGold.copy(alpha = 0.22f * (0.8f + 0.2f * lightBurstAlpha)),
                        MakiEmeraldLight.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.44f),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(w * 0.5f, h * 0.44f)
            )

            // Render floating golden bokeh particles
            particles.forEach { p ->
                val waveOffset = sin(ambientWave * p.speedFactor + p.phaseOffset)
                val cosOffset = cos(ambientWave * 0.8f * p.speedFactor + p.phaseOffset)
                val x = (p.initialXNorm * w) + (waveOffset * 18.dp.toPx())
                val y = (p.initialYNorm * h) - (progress * 40.dp.toPx()) + (cosOffset * 14.dp.toPx())
                val particleAlpha = (p.baseAlpha * (0.5f + 0.5f * sin(ambientWave + p.phaseOffset)))
                    .coerceIn(0f, 1f) * boxAlpha

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MakiGoldLight.copy(alpha = particleAlpha),
                            MakiChampagneGold.copy(alpha = particleAlpha * 0.7f),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = p.radiusDp.dp.toPx() * 2f
                    ),
                    radius = p.radiusDp.dp.toPx() * 1.5f,
                    center = Offset(x, y)
                )
            }
        }

        // --- 2. 3D GIFT BOX & RADIANT LIGHT EXPERIENCE ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer {
                        scaleX = boxScale
                        scaleY = boxScale
                        alpha = boxAlpha
                        rotationY = boxRotationY
                        rotationX = boxRotationX
                        cameraDistance = 16f * density
                    }
            ) {
                // 3D Gift Box Canvas with realistic lighting, ribbon, and opening lid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRealistic3DGiftBox(
                        lidLiftNorm = lidLiftNorm,
                        lightBurstAlpha = lightBurstAlpha
                    )
                }

                // Brand Emblem ascending from the golden light within the box
                if (brandAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = -55.dp.toPx() * brandRevealProgress
                                alpha = brandAlpha
                                scaleX = brandScale
                                scaleY = brandScale
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MakiEmeraldDark,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MakiChampagneGold),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(MakiEmeraldLight, MakiEmeraldDark)
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = "Maki Brands",
                                    tint = MakiChampagneGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- 3. LUXURY BRAND TYPOGRAPHY & TAGLINE ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(brandAlpha)
                    .scale(brandScale)
            ) {
                Text(
                    text = "MAKI BRANDS",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 4.5.sp,
                    color = MakiGoldLight
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Champagne Gold Tagline
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 1.dp)
                            .background(MakiChampagneGold.copy(alpha = 0.7f))
                    )

                    Text(
                        text = "GIFT MORE. SMILE MORE.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.5.sp,
                        color = MakiChampagneGold
                    )

                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 1.dp)
                            .background(MakiChampagneGold.copy(alpha = 0.7f))
                    )
                }
            }
        }

        // --- 4. TOP SKIP BUTTON ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MakiChampagneGold.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSplashFinished() }
            ) {
                Text(
                    text = "SKIP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MakiChampagneGold.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Draws the realistic 3D gift box with isometric perspective, satin ribbons,
 * glowing interior, lifting lid, and radiant volumetric light rays.
 */
private fun DrawScope.drawRealistic3DGiftBox(
    lidLiftNorm: Float,
    lightBurstAlpha: Float
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.56f

    val boxWidth = size.width * 0.46f
    val boxHeight = size.height * 0.34f
    val isoAngle = 0.46f // Isometric slant factor

    val dx = boxWidth * 0.5f
    val dy = boxWidth * 0.5f * isoAngle

    // Coordinates of Base Box Vertices
    val vBottom = Offset(cx, cy + boxHeight)
    val vLeftBottom = Offset(cx - dx, cy + boxHeight - dy)
    val vRightBottom = Offset(cx + dx, cy + boxHeight - dy)

    val vCenterTop = Offset(cx, cy)
    val vLeftTop = Offset(cx - dx, cy - dy)
    val vRightTop = Offset(cx + dx, cy - dy)
    val vBackTop = Offset(cx, cy - 2f * dy)

    // --- 1. Soft Ambient Ground Shadow ---
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(cx, cy + boxHeight + 8.dp.toPx()),
            radius = dx * 1.35f
        ),
        topLeft = Offset(cx - dx * 1.35f, cy + boxHeight - 8.dp.toPx()),
        size = Size(dx * 2.7f, 32.dp.toPx())
    )

    // --- 2. Left Face of Box Body (Darker Velvet Emerald) ---
    val leftFacePath = Path().apply {
        moveTo(vCenterTop.x, vCenterTop.y)
        lineTo(vLeftTop.x, vLeftTop.y)
        lineTo(vLeftBottom.x, vLeftBottom.y)
        lineTo(vBottom.x, vBottom.y)
        close()
    }
    drawPath(
        path = leftFacePath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF093126), Color(0xFF051E17)),
            start = vLeftTop,
            end = vBottom
        )
    )

    // Left face vertical satin gold ribbon
    val ribbonWidth = dx * 0.22f
    val lrvTop1 = Offset(cx - dx * 0.5f - ribbonWidth * 0.5f, (vCenterTop.y + vLeftTop.y) * 0.5f + ribbonWidth * 0.5f * isoAngle)
    val lrvTop2 = Offset(cx - dx * 0.5f + ribbonWidth * 0.5f, (vCenterTop.y + vLeftTop.y) * 0.5f - ribbonWidth * 0.5f * isoAngle)
    val lrvBot1 = Offset(lrvTop1.x, lrvTop1.y + boxHeight)
    val lrvBot2 = Offset(lrvTop2.x, lrvTop2.y + boxHeight)

    val leftRibbonVertical = Path().apply {
        moveTo(lrvTop1.x, lrvTop1.y)
        lineTo(lrvTop2.x, lrvTop2.y)
        lineTo(lrvBot2.x, lrvBot2.y)
        lineTo(lrvBot1.x, lrvBot1.y)
        close()
    }
    drawPath(
        path = leftRibbonVertical,
        brush = Brush.linearGradient(
            colors = listOf(MakiGoldLight, MakiChampagneGold, MakiGoldDark),
            start = lrvTop1,
            end = lrvBot2
        )
    )

    // --- 3. Right Face of Box Body (Lit Velvet Emerald) ---
    val rightFacePath = Path().apply {
        moveTo(vCenterTop.x, vCenterTop.y)
        lineTo(vRightTop.x, vRightTop.y)
        lineTo(vRightBottom.x, vRightBottom.y)
        lineTo(vBottom.x, vBottom.y)
        close()
    }
    drawPath(
        path = rightFacePath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF145C4B), Color(0xFF0D3E32)),
            start = vRightTop,
            end = vBottom
        )
    )

    // Right face vertical satin gold ribbon
    val rrvTop1 = Offset(cx + dx * 0.5f - ribbonWidth * 0.5f, (vCenterTop.y + vRightTop.y) * 0.5f - ribbonWidth * 0.5f * isoAngle)
    val rrvTop2 = Offset(cx + dx * 0.5f + ribbonWidth * 0.5f, (vCenterTop.y + vRightTop.y) * 0.5f + ribbonWidth * 0.5f * isoAngle)
    val rrvBot1 = Offset(rrvTop1.x, rrvTop1.y + boxHeight)
    val rrvBot2 = Offset(rrvTop2.x, rrvTop2.y + boxHeight)

    val rightRibbonVertical = Path().apply {
        moveTo(rrvTop1.x, rrvTop1.y)
        lineTo(rrvTop2.x, rrvTop2.y)
        lineTo(rrvBot2.x, rrvBot2.y)
        lineTo(rrvBot1.x, rrvBot1.y)
        close()
    }
    drawPath(
        path = rightRibbonVertical,
        brush = Brush.linearGradient(
            colors = listOf(MakiGoldLight, MakiChampagneGold, MakiGoldDark),
            start = rrvTop1,
            end = rrvBot2
        )
    )

    // --- 4. Radiant Glowing Golden Interior (Revealed as Lid Opens) ---
    val topCavityPath = Path().apply {
        moveTo(vCenterTop.x, vCenterTop.y)
        lineTo(vLeftTop.x, vLeftTop.y)
        lineTo(vBackTop.x, vBackTop.y)
        lineTo(vRightTop.x, vRightTop.y)
        close()
    }

    // Interior cavity fill with intense radiant gold illumination
    drawPath(
        path = topCavityPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF7D6),
                MakiGoldAccent,
                MakiChampagneGold,
                Color(0xFF6B510B)
            ),
            center = Offset(cx, cy - dy),
            radius = dx * 1.1f
        )
    )

    // --- 5. Volumetric Golden Light Beams shooting out from the open box ---
    if (lightBurstAlpha > 0.01f) {
        val beamCount = 9
        val beamMaxHeight = 160.dp.toPx() * lightBurstAlpha
        for (i in 0 until beamCount) {
            val angleNorm = (i.toFloat() / (beamCount - 1)) - 0.5f // -0.5 to +0.5
            val beamXSpread = angleNorm * dx * 2.8f
            val beamPath = Path().apply {
                moveTo(cx - 10.dp.toPx(), cy - dy)
                lineTo(cx + 10.dp.toPx(), cy - dy)
                lineTo(cx + beamXSpread + 22.dp.toPx(), cy - dy - beamMaxHeight)
                lineTo(cx + beamXSpread - 22.dp.toPx(), cy - dy - beamMaxHeight)
                close()
            }

            drawPath(
                path = beamPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MakiChampagneGold.copy(alpha = 0.32f * lightBurstAlpha),
                        Color(0xFFFFF9E6).copy(alpha = 0.55f * lightBurstAlpha)
                    ),
                    startY = cy - dy - beamMaxHeight,
                    endY = cy - dy
                )
            )
        }

        // Intense core golden flare at mouth of the box
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.90f * lightBurstAlpha),
                    MakiGoldLight.copy(alpha = 0.70f * lightBurstAlpha),
                    MakiChampagneGold.copy(alpha = 0.30f * lightBurstAlpha),
                    Color.Transparent
                ),
                center = Offset(cx, cy - dy),
                radius = dx * 0.95f * lightBurstAlpha
            ),
            radius = dx * 0.95f * lightBurstAlpha,
            center = Offset(cx, cy - dy)
        )
    }

    // --- 6. The 3D Box Lid (Lifts Upwards and Tilts Backward in 3D) ---
    // Lift distance up to 72dp, with 3D perspective slant
    val liftY = -72.dp.toPx() * lidLiftNorm
    val lidTiltX = -28.dp.toPx() * lidLiftNorm
    val lidTiltY = -14.dp.toPx() * lidLiftNorm

    val lidCenter = Offset(cx, cy + liftY)
    val lidLeft = Offset(cx - dx * 1.05f, cy - dy * 1.05f + liftY + lidTiltY)
    val lidRight = Offset(cx + dx * 1.05f, cy - dy * 1.05f + liftY - lidTiltY)
    val lidBack = Offset(cx, cy - 2f * dy * 1.05f + liftY + lidTiltX)

    // Lid Rim Height (thickness of the box lid)
    val lidThickness = 12.dp.toPx() * (1f - 0.25f * lidLiftNorm)

    // Lid Left Rim
    val lidLeftRim = Path().apply {
        moveTo(lidCenter.x, lidCenter.y)
        lineTo(lidLeft.x, lidLeft.y)
        lineTo(lidLeft.x, lidLeft.y + lidThickness)
        lineTo(lidCenter.x, lidCenter.y + lidThickness)
        close()
    }
    drawPath(
        path = lidLeftRim,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF093126), Color(0xFF061F18)),
            start = lidLeft,
            end = lidCenter
        )
    )

    // Lid Right Rim
    val lidRightRim = Path().apply {
        moveTo(lidCenter.x, lidCenter.y)
        lineTo(lidRight.x, lidRight.y)
        lineTo(lidRight.x, lidRight.y + lidThickness)
        lineTo(lidCenter.x, lidCenter.y + lidThickness)
        close()
    }
    drawPath(
        path = lidRightRim,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF145C4B), Color(0xFF0F473A)),
            start = lidCenter,
            end = lidRight
        )
    )

    // Lid Top Face
    val lidTopFace = Path().apply {
        moveTo(lidCenter.x, lidCenter.y)
        lineTo(lidLeft.x, lidLeft.y)
        lineTo(lidBack.x, lidBack.y)
        lineTo(lidRight.x, lidRight.y)
        close()
    }
    drawPath(
        path = lidTopFace,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF1A6B58), Color(0xFF0F473A)),
            start = lidLeft,
            end = lidRight
        )
    )

    // Lid Satin Gold Ribbon Cross
    val ribbonCross1 = Path().apply {
        val midL = Offset((lidCenter.x + lidLeft.x) * 0.5f, (lidCenter.y + lidLeft.y) * 0.5f)
        val midR = Offset((lidBack.x + lidRight.x) * 0.5f, (lidBack.y + lidRight.y) * 0.5f)
        moveTo(midL.x - 4f, midL.y - 2f)
        lineTo(midL.x + 4f, midL.y + 2f)
        lineTo(midR.x + 4f, midR.y + 2f)
        lineTo(midR.x - 4f, midR.y - 2f)
        close()
    }
    drawPath(
        path = ribbonCross1,
        brush = Brush.linearGradient(
            colors = listOf(MakiGoldLight, MakiChampagneGold, MakiGoldDark),
            start = lidLeft,
            end = lidRight
        )
    )

    // Luxurious 3D Gold Ribbon Bow on top of the lid
    val bowCenter = Offset((lidCenter.x + lidBack.x) * 0.5f, (lidCenter.y + lidBack.y) * 0.5f)
    val bowLoopScale = (1f - 0.4f * lidLiftNorm).coerceAtLeast(0.1f)

    // Left bow loop
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(MakiGoldLight, MakiChampagneGold, MakiGoldDark),
            center = bowCenter + Offset(-12.dp.toPx() * bowLoopScale, -10.dp.toPx() * bowLoopScale),
            radius = 16.dp.toPx() * bowLoopScale
        ),
        topLeft = bowCenter + Offset(-24.dp.toPx() * bowLoopScale, -18.dp.toPx() * bowLoopScale),
        size = Size(20.dp.toPx() * bowLoopScale, 14.dp.toPx() * bowLoopScale)
    )

    // Right bow loop
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(MakiGoldLight, MakiChampagneGold, MakiGoldDark),
            center = bowCenter + Offset(12.dp.toPx() * bowLoopScale, -10.dp.toPx() * bowLoopScale),
            radius = 16.dp.toPx() * bowLoopScale
        ),
        topLeft = bowCenter + Offset(4.dp.toPx() * bowLoopScale, -18.dp.toPx() * bowLoopScale),
        size = Size(20.dp.toPx() * bowLoopScale, 14.dp.toPx() * bowLoopScale)
    )

    // Center ribbon knot
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF4CC), MakiGoldAccent, MakiGoldDark),
            center = bowCenter,
            radius = 6.dp.toPx() * bowLoopScale
        ),
        radius = 5.5.dp.toPx() * bowLoopScale,
        center = bowCenter
    )
}
