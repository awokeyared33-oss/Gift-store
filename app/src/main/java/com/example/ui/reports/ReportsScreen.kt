package com.example.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.calculator.FinancialCalculator
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSecondaryBlue
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiSurfaceVariant
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.theme.MakiWarning
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.MakiViewModel
import java.math.BigDecimal

@Composable
fun ReportsScreen(
    viewModel: MakiViewModel,
    modifier: Modifier = Modifier
) {
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val performances by viewModel.productPerformances.collectAsStateWithLifecycle()
    val activeProducts by viewModel.activeProducts.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()

    val slowMovingProducts = remember(activeProducts, performances) {
        val soldNames = performances.map { it.productName }.toSet()
        activeProducts.filter { it.name !in soldNames }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = strings.reportsTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MakiPrimaryBlue
        )
        Text(
            text = "Business financials, inventory valuation & product analytics",
            fontSize = 13.sp,
            color = MakiTextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Date Filter Chips
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                DateFilterType.TODAY to strings.filterToday,
                DateFilterType.YESTERDAY to strings.filterYesterday,
                DateFilterType.THIS_WEEK to strings.filterThisWeek,
                DateFilterType.THIS_MONTH to strings.filterThisMonth,
                DateFilterType.LAST_3_MONTHS to strings.filterLast3Months,
                DateFilterType.LAST_6_MONTHS to strings.filterLast6Months,
                DateFilterType.THIS_YEAR to strings.filterThisYear,
                DateFilterType.ALL_TIME to strings.filterAllTime
            ).forEach { (filterType, label) ->
                val isSelected = (selectedFilter == filterType)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setDateFilter(filterType) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MakiPrimaryBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Financial Summary Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Revenue & Profit Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MakiTextDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Selected Filter Revenue", fontSize = 12.sp, color = MakiTextSecondary)
                                Text(
                                    FinancialCalculator.formatMoney(metrics.selectedFilterRevenue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MakiPrimaryBlue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Selected Gross Profit", fontSize = 12.sp, color = MakiTextSecondary)
                                Text(
                                    FinancialCalculator.formatMoney(metrics.selectedFilterProfit),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MakiSuccess
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MakiSurfaceBorder))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(strings.lifetimeRevenue, fontSize = 12.sp, color = MakiTextSecondary)
                                Text(
                                    FinancialCalculator.formatMoney(metrics.lifetimeRevenue),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MakiTextDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(strings.lifetimeGrossProfit, fontSize = 12.sp, color = MakiTextSecondary)
                                Text(
                                    FinancialCalculator.formatMoney(metrics.lifetimeProfit),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MakiSuccess
                                )
                            }
                        }
                    }
                }
            }

            // Visual Sales Performance Bar Chart
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Top Products Sales Volume",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MakiTextDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (performances.isEmpty()) {
                            Text("No sales data available for chart.", color = MakiTextSecondary, fontSize = 13.sp)
                        } else {
                            val topPerformers = performances.take(5)
                            val maxSold = (topPerformers.maxOfOrNull { it.totalQuantitySold } ?: 1).coerceAtLeast(1)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                topPerformers.forEach { item ->
                                    val ratio = (item.totalQuantitySold.toFloat() / maxSold).coerceIn(0.05f, 1f)
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(item.productName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${item.totalQuantitySold} sold (${FinancialCalculator.formatMoney(item.totalRevenue)})", fontSize = 12.sp, color = MakiTextSecondary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp)
                                        ) {
                                            // Background bar
                                            drawRoundRect(
                                                color = Color(0xFFF3F4F6),
                                                size = size,
                                                cornerRadius = CornerRadius(4.dp.toPx())
                                            )
                                            // Progress bar
                                            drawRoundRect(
                                                color = MakiPrimaryBlue,
                                                size = Size(width = size.width * ratio, height = size.height),
                                                cornerRadius = CornerRadius(4.dp.toPx())
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Inventory Valuation Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.inventoryValuation,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MakiTextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.inventoryCostValue, fontSize = 13.sp, color = MakiTextSecondary)
                            Text(
                                FinancialCalculator.formatMoney(metrics.totalInventoryCostValue),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MakiPrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.totalInventoryQty, fontSize = 13.sp, color = MakiTextSecondary)
                            Text("${metrics.totalInventoryStock} items", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Slow-Moving Products Section
            if (slowMovingProducts.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${strings.slowMovingProducts} (${slowMovingProducts.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MakiWarning
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            slowMovingProducts.take(5).forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${p.stockQuantity} in stock", fontSize = 12.sp, color = MakiTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
