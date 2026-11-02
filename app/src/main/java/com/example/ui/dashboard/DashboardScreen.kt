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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.example.ui.theme.MakiCardBorder
import com.example.ui.theme.MakiCharcoal
import com.example.ui.theme.MakiCharcoalLight
import com.example.ui.theme.MakiCharcoalMuted
import com.example.ui.theme.MakiChampagneGold
import com.example.ui.theme.MakiEmerald
import com.example.ui.theme.MakiEmeraldContainer
import com.example.ui.theme.MakiEmeraldDark
import com.example.ui.theme.MakiEmeraldLight
import com.example.ui.theme.MakiGoldAccent
import com.example.ui.theme.MakiGoldDark
import com.example.ui.theme.MakiGoldLight
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceWhite
import com.example.ui.theme.MakiWarmIvory
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
            .background(MakiWarmIvory)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. COMPACT LUXURY HEADER (Boutique Crest + Brand Title + Language Selector) ---
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Refined Emerald & Gold Boutique Crest
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(MakiEmerald, MakiEmeraldDark)
                                )
                            )
                            .border(1.dp, MakiChampagneGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Maki Brands Crest",
                            tint = MakiChampagneGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = strings.appName,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiCharcoal,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (isAmharic) "የስጦታ ሱቅ አስተዳደር" else "Luxury Gift Boutique CRM",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MakiCharcoalLight
                        )
                    }
                }

                // Compact Emerald/Gold Language Dropdown Pill
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, MakiEmerald.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .background(MakiSurfaceWhite)
                            .clickable { showLanguageDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("dashboard_lang_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAmharic) "አማ" else "EN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiEmerald
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Language",
                            tint = MakiEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showLanguageDropdown,
                        onDismissRequest = { showLanguageDropdown = false },
                        modifier = Modifier.background(MakiSurfaceWhite)
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

        // --- 2. EXECUTIVE QUICK ACTIONS (New Sale, Add Product, Add Customer) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: New Sale
                QuickActionButton(
                    title = if (isAmharic) "አዲስ ሽያጭ" else "New Sale",
                    icon = Icons.Default.AddShoppingCart,
                    containerColor = MakiEmerald,
                    contentColor = Color.White,
                    iconTint = MakiChampagneGold,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("dashboard_action_new_sale"),
                    onClick = onNavigateToSales
                )

                // Secondary Action: Add Product
                QuickActionButton(
                    title = if (isAmharic) "እቃ ጨምር" else "Product",
                    icon = Icons.Default.CardGiftcard,
                    containerColor = MakiSurfaceWhite,
                    contentColor = MakiCharcoal,
                    iconTint = MakiEmerald,
                    borderColor = MakiCardBorder,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_action_add_product"),
                    onClick = onNavigateToProducts
                )

                // Tertiary Action: Add Customer
                QuickActionButton(
                    title = if (isAmharic) "ደንበኛ" else "Customer",
                    icon = Icons.Default.PersonAdd,
                    containerColor = MakiSurfaceWhite,
                    contentColor = MakiCharcoal,
                    iconTint = MakiEmerald,
                    borderColor = MakiCardBorder,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_action_add_customer"),
                    onClick = onNavigateToCustomers
                )
            }
        }

        // --- 3. CORE FINANCIAL METRICS (Total Revenue & Total Profit) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Revenue Card
                FinancialMetricCard(
                    title = if (isAmharic) "የዛሬ ገቢ" else "Today's Revenue",
                    mainAmount = FinancialCalculator.formatBirr(metrics.todayRevenue),
                    secondaryLabel = if (isAmharic) "አጠቃላይ ገቢ" else "Total Revenue",
                    secondaryAmount = FinancialCalculator.formatBirr(metrics.lifetimeRevenue),
                    badgeText = "ETB",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconTint = MakiEmerald,
                    iconContainer = MakiEmeraldContainer,
                    modifier = Modifier.weight(1f)
                )

                // Total Profit Card
                FinancialMetricCard(
                    title = if (isAmharic) "የዛሬ ትርፍ" else "Today's Profit",
                    mainAmount = FinancialCalculator.formatBirr(metrics.todayProfit),
                    secondaryLabel = if (isAmharic) "አጠቃላይ ትርፍ" else "Total Profit",
                    secondaryAmount = FinancialCalculator.formatBirr(metrics.lifetimeProfit),
                    badgeText = "+Margin",
                    icon = Icons.Default.MonetizationOn,
                    iconTint = MakiGoldDark,
                    iconContainer = MakiGoldLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 4. VOLUME & ORDERS METRICS (Total Sales & Total Orders) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VolumeMetricCard(
                    title = if (isAmharic) "የዛሬ ሽያጮች" else "Today's Sales",
                    value = "${metrics.todaySalesCount}",
                    subText = if (isAmharic) "የተጠናቀቁ ትዕዛዞች" else "Completed Orders",
                    icon = Icons.Default.ShoppingBag,
                    iconTint = MakiEmerald,
                    iconBg = MakiEmeraldContainer,
                    modifier = Modifier.weight(1f)
                )

                VolumeMetricCard(
                    title = if (isAmharic) "የሱቅ እቃዎች" else "Active Catalog",
                    value = "${metrics.totalProductsCount}",
                    subText = if (isAmharic) "በመጋዘን ውስጥ ያሉ" else "In-Stock Gifts",
                    icon = Icons.Default.Inventory2,
                    iconTint = MakiGoldDark,
                    iconBg = MakiGoldLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 5. SALES OVERVIEW LINE CHART CARD ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MakiSurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, MakiCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_chart_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row: "Sales Overview" + Filter Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MakiEmerald)
                            )
                            Text(
                                text = if (isAmharic) "የሽያጭ አጠቃላይ እይታ" else "Sales Overview",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MakiCharcoal
                            )
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, MakiCardBorder, RoundedCornerShape(16.dp))
                                    .background(MakiWarmIvory)
                                    .clickable { showFilterDropdown = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
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
                                    fontWeight = FontWeight.SemiBold,
                                    color = MakiEmerald
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Filter",
                                    tint = MakiEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showFilterDropdown,
                                onDismissRequest = { showFilterDropdown = false },
                                modifier = Modifier.background(MakiSurfaceWhite)
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
                                        text = { Text(label, color = MakiCharcoal) },
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

        // --- 6. UPCOMING CELEBRATIONS SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MakiChampagneGold)
                        )
                        Text(
                            text = if (isAmharic) "ቀጣይ በዓላት" else "Upcoming Celebrations",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiCharcoal
                        )
                    }

                    TextButton(onClick = onNavigateToCustomers) {
                        Text(
                            text = if (isAmharic) "ሁሉንም እይ →" else "See All →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiEmerald
                        )
                    }
                }

                val topCelebration = upcomingCelebrations.firstOrNull()
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MakiSurfaceWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MakiCardBorder),
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
                                .background(MakiGoldLight)
                                .border(1.dp, MakiChampagneGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cake,
                                contentDescription = "Celebration",
                                tint = MakiGoldDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (topCelebration != null) {
                                    "${topCelebration.customerName}'s ${topCelebration.celebration.celebrationName}"
                                } else {
                                    if (isAmharic) "የአበበ እናት ልደት" else "Customer Birthday Reminder"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MakiCharcoal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (topCelebration != null) {
                                    "In ${topCelebration.daysUntil} days • ${topCelebration.celebration.celebrationName} (E.C.)"
                                } else {
                                    if (isAmharic) "በቅርቡ የሚመጣ በዓል የለም" else "Registered customer events appear here"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = MakiCharcoalLight
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "View",
                            tint = MakiEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Touch-friendly Quick Action button designed with luxury boutique styling
 */
@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = borderColor?.let { androidx.compose.foundation.BorderStroke(1.dp, it) },
        shadowElevation = if (containerColor != MakiSurfaceWhite) 2.dp else 0.dp,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * Premium Financial Metric Card for Revenue & Profit with Ethiopian Birr formatting
 */
@Composable
private fun FinancialMetricCard(
    title: String,
    mainAmount: String,
    secondaryLabel: String,
    secondaryAmount: String,
    badgeText: String,
    icon: ImageVector,
    iconTint: Color,
    iconContainer: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MakiSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, MakiCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MakiWarmIvory)
                        .border(1.dp, MakiCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MakiCharcoalLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MakiCharcoalLight
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mainAmount,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MakiCharcoal,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MakiCardBorder)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = secondaryLabel,
                    fontSize = 10.sp,
                    color = MakiCharcoalMuted
                )
                Text(
                    text = secondaryAmount,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiCharcoalLight
                )
            }
        }
    }
}

/**
 * Compact Volume Card for Today's Sales & Catalog counts
 */
@Composable
private fun VolumeMetricCard(
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MakiSurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, MakiCardBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MakiCharcoalLight
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiCharcoal
                )
                Text(
                    text = subText,
                    fontSize = 10.sp,
                    color = MakiCharcoalMuted
                )
            }
        }
    }
}

/**
 * Sales Line Chart Canvas rendering smooth bezier curve with champagne gold/emerald tones
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

            // Horizontal dotted grid lines
            val strokeDotted = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            for (i in 0..3) {
                val y = chartH * (i / 3f)
                drawLine(
                    color = Color(0xFFE5E9E5),
                    start = Offset(leftMargin, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                    pathEffect = strokeDotted.pathEffect
                )
            }

            // Trend points
            val pointsNorm = listOf(0.28f, 0.35f, 0.48f, 0.42f, 0.60f, 0.52f, 0.68f, 0.62f, 0.82f)

            val pointCoords = pointsNorm.mapIndexed { index, norm ->
                val x = leftMargin + (index.toFloat() / (pointsNorm.size - 1)) * chartW
                val y = chartH - (norm * chartH)
                Offset(x, y)
            }

            // Draw area gradient under line (Emerald fading into Ivory)
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
                    colors = listOf(MakiEmerald.copy(alpha = 0.22f), MakiChampagneGold.copy(alpha = 0.05f), Color.Transparent),
                    startY = 0f,
                    endY = chartH
                )
            )

            // Draw line in luxury emerald
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
                brush = Brush.horizontalGradient(
                    colors = listOf(MakiEmerald, MakiEmeraldLight, MakiChampagneGold)
                ),
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw circular dots with gold accent centers
            val keyIndices = listOf(0, 2, 4, 6, 8)
            for (idx in keyIndices) {
                val pt = pointCoords[idx]
                drawCircle(color = MakiSurfaceWhite, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = MakiEmerald, radius = 3.5.dp.toPx(), center = pt)
                drawCircle(color = MakiChampagneGold, radius = 1.5.dp.toPx(), center = pt)
            }
        }

        // Labels overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(110.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("3k", fontSize = 10.sp, color = MakiCharcoalMuted)
            Text("2k", fontSize = 10.sp, color = MakiCharcoalMuted)
            Text("1k", fontSize = 10.sp, color = MakiCharcoalMuted)
            Text("0k", fontSize = 10.sp, color = MakiCharcoalMuted)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1", fontSize = 10.sp, color = MakiCharcoalMuted)
            Text("7", fontSize = 10.sp, color = MakiCharcoalMuted)
            Text("14", fontSize = 10.sp, color = MakiCharcoalMuted)
            Text("21", fontSize = 10.sp, color = MakiCharcoalMuted)
            Text("30", fontSize = 10.sp, color = MakiCharcoalMuted)
        }
    }
}
