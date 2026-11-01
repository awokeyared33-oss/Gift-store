package com.example.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SaleEntity
import com.example.domain.calculator.FinancialCalculator
import com.example.ui.theme.MakiError
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiSurfaceVariant
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.MakiViewModel
import java.math.BigDecimal

@Composable
fun SalesHistoryScreen(
    viewModel: MakiViewModel,
    modifier: Modifier = Modifier
) {
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val allSales by viewModel.filteredSales.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()

    var productSearchQuery by remember { mutableStateOf("") }
    var selectedSaleForDetails by remember { mutableStateOf<SaleEntity?>(null) }
    var saleToVoid by remember { mutableStateOf<SaleEntity?>(null) }
    var voidReason by remember { mutableStateOf("") }

    val displayedSales = remember(allSales, productSearchQuery) {
        if (productSearchQuery.isBlank()) allSales
        else allSales.filter {
            it.productNameSnapshot.contains(productSearchQuery, ignoreCase = true) ||
                    (it.customerNameSnapshot?.contains(productSearchQuery, ignoreCase = true) == true)
        }
    }

    // Filtered Totals
    val validSales = displayedSales.filter { !it.isVoided }
    val totalRevenue = remember(validSales) {
        var sum = BigDecimal.ZERO
        validSales.forEach { sum = sum.add(BigDecimal.valueOf(it.totalRevenue)) }
        sum
    }
    val totalProfit = remember(validSales) {
        var sum = BigDecimal.ZERO
        validSales.forEach { sum = sum.add(BigDecimal.valueOf(it.grossProfit)) }
        sum
    }
    val totalQuantitySold = remember(validSales) {
        validSales.sumOf { it.quantity }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = strings.historyTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MakiPrimaryBlue
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

        Spacer(modifier = Modifier.height(10.dp))

        // Search in history
        OutlinedTextField(
            value = productSearchQuery,
            onValueChange = { productSearchQuery = it },
            placeholder = { Text("Filter by product or customer...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MakiTextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("history_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filtered Totals Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MakiSurfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("${strings.totalTransactions}: ${validSales.size} (${totalQuantitySold} items)", fontSize = 12.sp, color = MakiTextSecondary)
                    Text("Total Revenue: ${FinancialCalculator.formatMoney(totalRevenue)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MakiPrimaryBlue)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Gross Profit", fontSize = 12.sp, color = MakiTextSecondary)
                    Text("+${FinancialCalculator.formatMoney(totalProfit)}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MakiSuccess)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Structured Table Header (Responsive)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(strings.dateCol, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
            Text(strings.productCol, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
            Text(strings.qtyCol, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center)
            Text(strings.revenueCol, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
            Text(strings.profitCol, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sales List
        if (displayedSales.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.noRecentSales, color = MakiTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayedSales, key = { it.id }) { sale ->
                    SalesHistoryRow(
                        sale = sale,
                        strings = strings,
                        onClick = { selectedSaleForDetails = sale }
                    )
                }
            }
        }
    }

    // Sale Details & Void Modal
    selectedSaleForDetails?.let { sale ->
        AlertDialog(
            onDismissRequest = { selectedSaleForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MakiPrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sale #${sale.id} Details", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Product: ${sale.productNameSnapshot}", fontWeight = FontWeight.Bold)
                    Text("Date: ${sale.ethiopianDateStr}")
                    Text("Quantity Sold: ${sale.quantity}")
                    Text("Historical Purchase Price: ${FinancialCalculator.formatMoney(BigDecimal.valueOf(sale.purchasePriceSnapshot))}")
                    Text("Historical Selling Price: ${FinancialCalculator.formatMoney(BigDecimal.valueOf(sale.sellingPriceSnapshot))}")
                    Text("Total Revenue: ${FinancialCalculator.formatMoney(BigDecimal.valueOf(sale.totalRevenue))}")
                    Text("Cost of Goods Sold: ${FinancialCalculator.formatMoney(BigDecimal.valueOf(sale.totalCost))}")
                    Text("Gross Profit: ${FinancialCalculator.formatMoney(BigDecimal.valueOf(sale.grossProfit))}", color = MakiSuccess, fontWeight = FontWeight.Bold)

                    if (sale.customerNameSnapshot != null) {
                        Text("Customer: ${sale.customerNameSnapshot}")
                    }
                    if (sale.notes.isNotBlank()) {
                        Text("Notes: ${sale.notes}")
                    }

                    if (sale.isVoided) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF2F2))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "VOIDED / REFUNDED: ${sale.voidReason}",
                                color = MakiError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSaleForDetails = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                if (!sale.isVoided) {
                    Button(
                        onClick = {
                            saleToVoid = sale
                            selectedSaleForDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MakiError)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.voidSale)
                    }
                }
            }
        )
    }

    // Void Sale Confirmation Dialog
    saleToVoid?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToVoid = null },
            title = { Text(strings.voidSale, color = MakiError, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.voidSaleConfirm)
                    Text("Product: ${sale.productNameSnapshot} (${sale.quantity} units will be returned to stock)")
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text(strings.voidReason) },
                        modifier = Modifier.fillMaxWidth().testTag("void_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = if (voidReason.isBlank()) "Customer Refund" else voidReason
                        viewModel.voidSale(sale.id, reason) {
                            saleToVoid = null
                            voidReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MakiError),
                    modifier = Modifier.testTag("confirm_void_sale_button")
                ) {
                    Text("Confirm Void")
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToVoid = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
fun SalesHistoryRow(
    sale: SaleEntity,
    strings: com.example.localization.AppStrings,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (sale.isVoided) Color(0xFFFEF2F2) else Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (sale.isVoided) Color(0xFFFECACA) else MakiSurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = sale.ethiopianDateStr,
                    fontSize = 11.sp,
                    color = MakiTextDark,
                    fontWeight = FontWeight.Medium
                )
                if (sale.isVoided) {
                    Text(
                        text = strings.voidedBadge,
                        fontSize = 9.sp,
                        color = MakiError,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = sale.productNameSnapshot,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MakiTextDark
                )
                if (sale.customerNameSnapshot != null) {
                    Text(
                        text = sale.customerNameSnapshot!!,
                        fontSize = 10.sp,
                        color = MakiTextSecondary
                    )
                }
            }

            Text(
                text = "${sale.quantity}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.Center,
                color = MakiTextDark
            )

            Text(
                text = FinancialCalculator.formatMoneyWithoutCurrency(BigDecimal.valueOf(sale.totalRevenue)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.2f),
                textAlign = TextAlign.End,
                color = if (sale.isVoided) MakiTextSecondary else MakiPrimaryBlue
            )

            Text(
                text = "+${FinancialCalculator.formatMoneyWithoutCurrency(BigDecimal.valueOf(sale.grossProfit))}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.2f),
                textAlign = TextAlign.End,
                color = if (sale.isVoided) MakiTextSecondary else MakiSuccess
            )
        }
    }
}
