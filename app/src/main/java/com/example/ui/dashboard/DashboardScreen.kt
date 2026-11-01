package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.calculator.FinancialCalculator
import com.example.localization.AppLanguage
import com.example.ui.components.HeroGiftBoxesIllustration
import com.example.ui.theme.MakiGold
import com.example.ui.theme.MakiLightBlue
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSecondaryBlue
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.theme.MakiTextTertiary
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.MakiViewModel

@Composable
fun DashboardScreen(
    viewModel: MakiViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAmharic = (currentLang == AppLanguage.AMHARIC)

    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
    val upcomingCelebrations by viewModel.upcomingCelebrationItems.collectAsStateWithLifecycle()

    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showFilterDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TOP HEADER (Gift Icon + Brand + Language Dropdown Pill) ---
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MakiLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Maki Brands",
                            tint = MakiPrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = strings.appName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiPrimaryBlue
                        )
                        Text(
                            text = if (isAmharic) "የስጦታ ሱቅ አስተዳደር" else "Gift Shop Management",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MakiTextSecondary
                        )
                    }
                }

                // Language dropdown pill: "EN ▾" or "አማ ▾"
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable { showLanguageDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("dashboard_lang_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAmharic) "አማ" else "EN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiPrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Language",
                            tint = MakiPrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageDropdown,
                        onDismissRequest = { showLanguageDropdown = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("English (EN)", fontWeight = if (!isAmharic) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setLanguage(AppLanguage.ENGLISH)
                                showLanguageDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("አማርኛ (Amharic)", fontWeight = if (isAmharic) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                viewModel.setLanguage(AppLanguage.AMHARIC)
                                showLanguageDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // --- 2. HERO REVENUE CARD (Blue gradient + 3D Gift Box illustration) ---
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(MakiPrimaryBlue, MakiSecondaryBlue)
                        )
                    )
                    .clickable { onNavigateToSales() }
                    .testTag("dashboard_hero_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAmharic) "የዛሬ ገቢ" else "Today's Revenue",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FinancialCalculator.formatBirr(metrics.todayRevenue),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "↑ +12%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 3D Gift Boxes Canvas Illustration
                    HeroGiftBoxesIllustration(
                        modifier = Modifier
                            .size(116.dp)
                            .padding(start = 8.dp)
                    )
                }
            }
        }

        // --- 3. 2x2 METRICS GRID (Today's Profit, Today's Sales, Total Revenue, Total Profit) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = MakiSuccess,
                        iconBg = Color(0xFFDCFCE7),
                        title = if (isAmharic) "የዛሬ ትርፍ" else "Today's Profit",
                        value = FinancialCalculator.formatBirr(metrics.todayProfit),
                        modifier = Modifier.weight(1f)
                    )

                    MetricMiniCard(
                        icon = Icons.Default.ShoppingCart,
                        iconTint = MakiPrimaryBlue,
                        iconBg = MakiLightBlue,
                        title = if (isAmharic) "የዛሬ ሽያጮች" else "Today's Sales",
                        value = "${metrics.todaySalesCount}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        icon = Icons.Default.AttachMoney,
                        iconTint = MakiSuccess,
                        iconBg = Color(0xFFDCFCE7),
                        title = if (isAmharic) "አጠቃላይ ገቢ" else "Total Revenue",
                        value = FinancialCalculator.formatBirr(metrics.lifetimeRevenue),
                        modifier = Modifier.weight(1f)
                    )

                    MetricMiniCard(
                        icon = Icons.Default.EmojiEvents,
                        iconTint = MakiGold,
                        iconBg = Color(0xFFFEF3C7),
                        title = if (isAmharic) "አጠቃላይ ትርፍ" else "Total Profit",
                        value = FinancialCalculator.formatBirr(metrics.lifetimeProfit),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 4. SALES OVERVIEW LINE CHART CARD ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_chart_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row: "Sales Overview" + "This Month ▾" dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAmharic) "የሽያጭ አጠቃላይ እይታ" else "Sales Overview",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiTextDark
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(16.dp))
                                    .clickable { showFilterDropdown = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (selectedFilter) {
                                        DateFilterType.TODAY -> if (isAmharic) "ዛሬ" else "Today"
                                        DateFilterType.YESTERDAY -> if (isAmharic) "ትናንት" else "Yesterday"
                                        DateFilterType.THIS_WEEK -> if (isAmharic) "በዚህ ሳምንት" else "This Week"
                                        DateFilterType.THIS_MONTH -> if (isAmharic) "በዚህ ወር" else "This Month"
                                        DateFilterType.LAST_3_MONTHS -> if (isAmharic) "የ 3 ወር" else "3 Months"
                                        DateFilterType.LAST_6_MONTHS -> if (isAmharic) "የ 6 ወር" else "6 Months"
                                        DateFilterType.THIS_YEAR -> if (isAmharic) "በዚህ ዓመት" else "This Year"
                                        DateFilterType.ALL_TIME -> if (isAmharic) "ሁልጊዜ" else "All Time"
                                        DateFilterType.CUSTOM -> if (isAmharic) "ብጁ" else "Custom"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MakiPrimaryBlue
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Filter",
                                    tint = MakiPrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showFilterDropdown,
                                onDismissRequest = { showFilterDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                listOf(
                                    DateFilterType.TODAY to (if (isAmharic) "ዛሬ" else "Today"),
                                    DateFilterType.THIS_WEEK to (if (isAmharic) "በዚህ ሳምንት" else "This Week"),
                                    DateFilterType.THIS_MONTH to (if (isAmharic) "በዚህ ወር" else "This Month"),
                                    DateFilterType.LAST_3_MONTHS to (if (isAmharic) "የ 3 ወር" else "3 Months"),
                                    DateFilterType.LAST_6_MONTHS to (if (isAmharic) "የ 6 ወር" else "6 Months"),
                                    DateFilterType.THIS_YEAR to (if (isAmharic) "በዚህ ዓመት" else "This Year"),
                                    DateFilterType.ALL_TIME to (if (isAmharic) "ሁልጊዜ" else "All Time")
                                ).forEach { (type, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDateFilter(type)
                                            showFilterDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Line chart Canvas
                    SalesLineChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )
                }
            }
        }

        // --- 5. UPCOMING CELEBRATIONS SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAmharic) "ቀጣይ በዓላት" else "Upcoming Celebrations",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MakiTextDark
                    )

                    TextButton(onClick = onNavigateToCustomers) {
                        Text(
                            text = if (isAmharic) "ሁሉንም እይ →" else "See All →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiPrimaryBlue
                        )
                    }
                }

                val topCelebration = upcomingCelebrations.firstOrNull()
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCustomers() }
                        .testTag("dashboard_celebration_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = "Celebration",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (topCelebration != null) {
                                    "${topCelebration.customerName}'s ${topCelebration.celebration.celebrationName}"
                                } else {
                                    if (isAmharic) "የአበበ እናት ልደት" else "Abebe's Mother Birthday"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MakiTextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (topCelebration != null) {
                                    "In ${topCelebration.daysUntil} days • ${topCelebration.celebration.celebrationName} (E.C.)"
                                } else {
                                    "In 7 days • Nov 8, 2026 (E.C.)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MakiTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View",
                            tint = MakiPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MetricMiniCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MakiTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MakiTextDark
            )
        }
    }
}

/**
 * Line chart canvas rendering the smooth curved sales overview with dots and fill.
 */
@Composable
fun SalesLineChart(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val leftMargin = 32.dp.toPx()
            val bottomMargin = 20.dp.toPx()
            val chartW = w - leftMargin
            val chartH = h - bottomMargin

            // Horizontal dotted grid lines (for 0k, 1k, 2k, 3k)
            val strokeDotted = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            for (i in 0..3) {
                val y = chartH * (i / 3f)
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(leftMargin, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                    pathEffect = strokeDotted.pathEffect
                )
            }

            // Data points matching mockup: 1, 7, 14, 21, 30
            // Normalized values (0.0 to 1.0)
            val pointsNorm = listOf(
                0.28f, // 1
                0.32f, // ~4
                0.48f, // 7
                0.42f, // ~10
                0.58f, // 14
                0.50f, // ~18
                0.66f, // 21
                0.60f, // ~25
                0.78f  // 30
            )

            val pointCoords = pointsNorm.mapIndexed { index, norm ->
                val x = leftMargin + (index.toFloat() / (pointsNorm.size - 1)) * chartW
                val y = chartH - (norm * chartH)
                Offset(x, y)
            }

            // Draw area gradient under line
            val areaPath = Path().apply {
                moveTo(pointCoords.first().x, chartH)
                lineTo(pointCoords.first().x, pointCoords.first().y)
                for (i in 1 until pointCoords.size) {
                    val prev = pointCoords[i - 1]
                    val curr = pointCoords[i]
                    val cX = (prev.x + curr.x) / 2
                    cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                }
                lineTo(pointCoords.last().x, chartH)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(MakiPrimaryBlue.copy(alpha = 0.20f), Color.Transparent),
                    startY = 0f,
                    endY = chartH
                )
            )

            // Draw line
            val linePath = Path().apply {
                moveTo(pointCoords.first().x, pointCoords.first().y)
                for (i in 1 until pointCoords.size) {
                    val prev = pointCoords[i - 1]
                    val curr = pointCoords[i]
                    val cX = (prev.x + curr.x) / 2
                    cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                }
            }

            drawPath(
                path = linePath,
                color = MakiPrimaryBlue,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw circular dots at key markers
            val keyIndices = listOf(0, 2, 4, 6, 8)
            for (idx in keyIndices) {
                val pt = pointCoords[idx]
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = MakiPrimaryBlue, radius = 3.5.dp.toPx(), center = pt)
            }
        }

        // Labels overlay (Y-axis: 3k, 2k, 1k, 0k on left, X-axis: 1, 7, 14, 21, 30 on bottom)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(110.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("3k", fontSize = 10.sp, color = MakiTextTertiary)
            Text("2k", fontSize = 10.sp, color = MakiTextTertiary)
            Text("1k", fontSize = 10.sp, color = MakiTextTertiary)
            Text("0k", fontSize = 10.sp, color = MakiTextTertiary)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1", fontSize = 10.sp, color = MakiTextTertiary)
            Text("7", fontSize = 10.sp, color = MakiTextTertiary)
            Text("14", fontSize = 10.sp, color = MakiTextTertiary)
            Text("21", fontSize = 10.sp, color = MakiTextTertiary)
            Text("30", fontSize = 10.sp, color = MakiTextTertiary)
        }
    }
}
