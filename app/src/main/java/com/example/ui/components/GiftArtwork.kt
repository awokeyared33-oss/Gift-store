package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Draws the 3D-style wrapped gift boxes seen in the Dashboard Hero Card.
 */
@Composable
fun HeroGiftBoxesIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Background subtle glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(w * 0.55f, h * 0.5f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.55f, h * 0.5f)
        )

        // --- Back Gift Box (Golden/Beige with blue ribbon) ---
        val backBoxLeft = w * 0.50f
        val backBoxTop = h * 0.28f
        val backBoxWidth = w * 0.44f
        val backBoxHeight = h * 0.52f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFD97706)),
                start = Offset(backBoxLeft, backBoxTop),
                end = Offset(backBoxLeft + backBoxWidth, backBoxTop + backBoxHeight)
            ),
            topLeft = Offset(backBoxLeft, backBoxTop),
            size = Size(backBoxWidth, backBoxHeight),
            cornerRadius = CornerRadius(12f, 12f)
        )
        // Ribbon vertical
        drawRect(
            color = Color(0xFF0866F5),
            topLeft = Offset(backBoxLeft + backBoxWidth * 0.42f, backBoxTop),
            size = Size(backBoxWidth * 0.16f, backBoxHeight)
        )
        // Ribbon horizontal
        drawRect(
            color = Color(0xFF0866F5),
            topLeft = Offset(backBoxLeft, backBoxTop + backBoxHeight * 0.42f),
            size = Size(backBoxWidth, backBoxHeight * 0.16f)
        )

        // --- Front Main Gift Box (White with Royal Blue ribbon) ---
        val frontBoxLeft = w * 0.12f
        val frontBoxTop = h * 0.20f
        val frontBoxWidth = w * 0.55f
        val frontBoxHeight = h * 0.68f

        // Soft drop shadow
        drawRoundRect(
            color = Color(0x33000000),
            topLeft = Offset(frontBoxLeft + 6f, frontBoxTop + 10f),
            size = Size(frontBoxWidth, frontBoxHeight),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // White box body
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
                start = Offset(frontBoxLeft, frontBoxTop),
                end = Offset(frontBoxLeft + frontBoxWidth, frontBoxTop + frontBoxHeight)
            ),
            topLeft = Offset(frontBoxLeft, frontBoxTop),
            size = Size(frontBoxWidth, frontBoxHeight),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // Vertical Blue Ribbon
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF0866F5), Color(0xFF2878E8), Color(0xFF0866F5))
            ),
            topLeft = Offset(frontBoxLeft + frontBoxWidth * 0.40f, frontBoxTop),
            size = Size(frontBoxWidth * 0.20f, frontBoxHeight)
        )

        // Horizontal Blue Ribbon
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0866F5), Color(0xFF2878E8), Color(0xFF0866F5))
            ),
            topLeft = Offset(frontBoxLeft, frontBoxTop + frontBoxHeight * 0.40f),
            size = Size(frontBoxWidth, frontBoxHeight * 0.20f)
        )

        // Silk Ribbon Bow on top
        val bowCenterX = frontBoxLeft + frontBoxWidth * 0.50f
        val bowCenterY = frontBoxTop

        // Left bow loop
        val leftBow = Path().apply {
            moveTo(bowCenterX, bowCenterY)
            cubicTo(
                bowCenterX - 28f, bowCenterY - 26f,
                bowCenterX - 36f, bowCenterY - 6f,
                bowCenterX, bowCenterY
            )
        }
        drawPath(leftBow, color = Color(0xFF2878E8))
        drawPath(leftBow, color = Color(0xFF0546B0), style = Stroke(2f))

        // Right bow loop
        val rightBow = Path().apply {
            moveTo(bowCenterX, bowCenterY)
            cubicTo(
                bowCenterX + 28f, bowCenterY - 26f,
                bowCenterX + 36f, bowCenterY - 6f,
                bowCenterX, bowCenterY
            )
        }
        drawPath(rightBow, color = Color(0xFF2878E8))
        drawPath(rightBow, color = Color(0xFF0546B0), style = Stroke(2f))

        // Center knot
        drawCircle(
            color = Color(0xFF0866F5),
            radius = 7f,
            center = Offset(bowCenterX, bowCenterY)
        )

        // Floating small blue gift box in front right
        val smallBoxLeft = w * 0.52f
        val smallBoxTop = h * 0.58f
        val smallBoxSize = w * 0.32f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
            ),
            topLeft = Offset(smallBoxLeft, smallBoxTop),
            size = Size(smallBoxSize, smallBoxSize),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // White ribbon on small box
        drawRect(
            color = Color.White,
            topLeft = Offset(smallBoxLeft + smallBoxSize * 0.42f, smallBoxTop),
            size = Size(smallBoxSize * 0.16f, smallBoxSize)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(smallBoxLeft, smallBoxTop + smallBoxSize * 0.42f),
            size = Size(smallBoxSize, smallBoxSize * 0.16f)
        )
    }
}

/**
 * Product visual thumbnail rendering custom gift artwork:
 * - Chocolate Box (dark box with chocolates and silk red ribbon)
 * - Teddy Bear (cute plush bear with red heart)
 * - Flower Bouquet (fresh pink roses with craft paper wrapping)
 * - Blue Gift Box (classic gift box with blue ribbon)
 */
@Composable
fun ProductThumbnailArt(
    productName: String,
    category: String,
    modifier: Modifier = Modifier
) {
    val norm = (productName + " " + category).lowercase()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        when {
            norm.contains("choc") -> ChocolateBoxIllustration(Modifier.fillMaxSize())
            norm.contains("bear") || norm.contains("toy") || norm.contains("plush") -> TeddyBearIllustration(Modifier.fillMaxSize())
            norm.contains("flower") || norm.contains("rose") || norm.contains("bouquet") -> FlowerBouquetIllustration(Modifier.fillMaxSize())
            else -> BlueGiftBoxIllustration(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ChocolateBoxIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Dark luxury chocolate box container
        val pad = w * 0.10f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3B2F2F), Color(0xFF1E140F))
            ),
            topLeft = Offset(pad, pad),
            size = Size(w - pad * 2, h - pad * 2),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Gold border inner line
        drawRoundRect(
            color = Color(0xFFD4AF37),
            topLeft = Offset(pad + 4f, pad + 4f),
            size = Size(w - pad * 2 - 8f, h - pad * 2 - 8f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(1.5f)
        )

        // Chocolate compartments (grid of 4 or 6 pralines)
        val cols = 3
        val rows = 2
        val cellW = (w - pad * 2 - 20f) / cols
        val cellH = (h - pad * 2 - 20f) / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cx = pad + 10f + c * cellW + cellW / 2
                val cy = pad + 10f + r * cellH + cellH / 2
                val chocoColor = if ((r + c) % 2 == 0) Color(0xFF5A2A18) else Color(0xFF8B4513)
                drawCircle(
                    color = chocoColor,
                    radius = cellW * 0.32f,
                    center = Offset(cx, cy)
                )
                // tiny shine
                drawCircle(
                    color = Color(0x66FFFFFF),
                    radius = cellW * 0.08f,
                    center = Offset(cx - cellW * 0.10f, cy - cellW * 0.10f)
                )
            }
        }

        // Satin Red Ribbon Bow on top-left corner
        val bowX = pad + 8f
        val bowY = pad + 8f
        val redRibbon = Color(0xFFDC2626)

        drawCircle(color = redRibbon, radius = 9f, center = Offset(bowX, bowY))
        val p1 = Path().apply {
            moveTo(bowX, bowY)
            cubicTo(bowX - 18f, bowY + 22f, bowX + 6f, bowY + 28f, bowX, bowY)
        }
        val p2 = Path().apply {
            moveTo(bowX, bowY)
            cubicTo(bowX + 22f, bowY - 18f, bowX + 28f, bowY + 6f, bowX, bowY)
        }
        drawPath(p1, redRibbon)
        drawPath(p2, redRibbon)
    }
}

@Composable
fun TeddyBearIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.55f

        val bearFur = Color(0xFFD4A373)
        val bearFurDark = Color(0xFFBC6C25)
        val bearInnerEar = Color(0xFFE9EDC9)

        // Ears
        drawCircle(bearFur, radius = w * 0.12f, center = Offset(cx - w * 0.22f, cy - h * 0.30f))
        drawCircle(bearInnerEar, radius = w * 0.06f, center = Offset(cx - w * 0.22f, cy - h * 0.30f))
        drawCircle(bearFur, radius = w * 0.12f, center = Offset(cx + w * 0.22f, cy - h * 0.30f))
        drawCircle(bearInnerEar, radius = w * 0.06f, center = Offset(cx + w * 0.22f, cy - h * 0.30f))

        // Body
        drawOval(bearFur, topLeft = Offset(cx - w * 0.25f, cy - h * 0.05f), size = Size(w * 0.50f, h * 0.45f))

        // Head
        drawCircle(bearFur, radius = w * 0.28f, center = Offset(cx, cy - h * 0.16f))

        // Muzzle
        drawOval(bearInnerEar, topLeft = Offset(cx - w * 0.12f, cy - h * 0.14f), size = Size(w * 0.24f, h * 0.16f))
        // Nose
        drawOval(Color(0xFF332014), topLeft = Offset(cx - w * 0.04f, cy - h * 0.12f), size = Size(w * 0.08f, h * 0.05f))

        // Eyes
        drawCircle(Color(0xFF1E140F), radius = 3.5f, center = Offset(cx - w * 0.10f, cy - h * 0.20f))
        drawCircle(Color(0xFF1E140F), radius = 3.5f, center = Offset(cx + w * 0.10f, cy - h * 0.20f))

        // Plush Red Heart held in arms
        val heartPath = Path().apply {
            val hx = cx
            val hy = cy + h * 0.10f
            val hw = w * 0.28f
            moveTo(hx, hy)
            cubicTo(hx - hw * 0.5f, hy - hw * 0.6f, hx - hw, hy + hw * 0.1f, hx, hy + hw * 0.8f)
            cubicTo(hx + hw, hy + hw * 0.1f, hx + hw * 0.5f, hy - hw * 0.6f, hx, hy)
        }
        drawPath(heartPath, color = Color(0xFFEF4444))
    }
}

@Composable
fun FlowerBouquetIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // Paper wrapping cone (Kraft/pink paper)
        val wrapPath = Path().apply {
            moveTo(cx, h * 0.90f)
            lineTo(cx - w * 0.35f, h * 0.45f)
            lineTo(cx + w * 0.35f, h * 0.45f)
            close()
        }
        drawPath(
            wrapPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFBCFE8), Color(0xFFF472B6))
            )
        )

        // Bouquet Ribbon
        drawRect(
            color = Color(0xFF0866F5),
            topLeft = Offset(cx - w * 0.15f, h * 0.62f),
            size = Size(w * 0.30f, 6.dp.toPx())
        )

        // Green leaves behind flowers
        drawCircle(Color(0xFF22C55E), radius = w * 0.12f, center = Offset(cx - w * 0.25f, h * 0.38f))
        drawCircle(Color(0xFF16A34A), radius = w * 0.12f, center = Offset(cx + w * 0.25f, h * 0.38f))

        // Cluster of Pink & White Roses
        drawCircle(Color(0xFFFB7185), radius = w * 0.14f, center = Offset(cx - w * 0.14f, h * 0.32f))
        drawCircle(Color(0xFFFFF1F2), radius = w * 0.13f, center = Offset(cx + w * 0.14f, h * 0.30f))
        drawCircle(Color(0xFFF43F5E), radius = w * 0.16f, center = Offset(cx, h * 0.22f))
        drawCircle(Color(0xFFFDA4AF), radius = w * 0.11f, center = Offset(cx, h * 0.38f))
    }
}

@Composable
fun BlueGiftBoxIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val pad = w * 0.14f
        val boxW = w - pad * 2
        val boxH = h - pad * 2

        // Soft drop shadow
        drawRoundRect(
            color = Color(0x1A000000),
            topLeft = Offset(pad + 4f, pad + 6f),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Blue Gift Box body
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0866F5), Color(0xFF2878E8)),
                start = Offset(pad, pad),
                end = Offset(pad + boxW, pad + boxH)
            ),
            topLeft = Offset(pad, pad),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // White/Gold Silk Ribbons
        val ribbonColor = Color.White
        drawRect(
            color = ribbonColor,
            topLeft = Offset(pad + boxW * 0.42f, pad),
            size = Size(boxW * 0.16f, boxH)
        )
        drawRect(
            color = ribbonColor,
            topLeft = Offset(pad, pad + boxH * 0.42f),
            size = Size(boxW, boxH * 0.16f)
        )

        // Ribbon Bow
        val bowCx = pad + boxW * 0.5f
        val bowCy = pad
        drawCircle(color = Color(0xFFFDE047), radius = 7f, center = Offset(bowCx, bowCy))
        drawCircle(color = ribbonColor, radius = 5f, center = Offset(bowCx, bowCy))
    }
}
