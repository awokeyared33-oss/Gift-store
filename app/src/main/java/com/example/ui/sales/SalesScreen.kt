package com.example.ui.sales

import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.domain.calculator.FinancialCalculator
import com.example.ui.components.ProductThumbnailArt
import com.example.ui.theme.MakiError
import com.example.ui.theme.MakiLightBlue
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.theme.MakiTextTertiary
import com.example.ui.viewmodel.MakiViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesScreen(
    viewModel: MakiViewModel,
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val activeProducts by viewModel.activeProducts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()

    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var quantityToSell by remember { mutableIntStateOf(1) }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }

    var showCustomerPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var completedSaleReceipt by remember { mutableStateOf<SaleEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-select first active product if none selected
    LaunchedEffect(activeProducts) {
        if (selectedProduct == null && activeProducts.isNotEmpty()) {
            selectedProduct = activeProducts.first()
        }
    }

    val prod = selectedProduct
    val selCustomer = customers.firstOrNull { it.id == selectedCustomerId }

    val revenue = if (prod != null) {
        FinancialCalculator.calculateRevenue(quantityToSell, BigDecimal.valueOf(prod.sellingPrice))
    } else BigDecimal.ZERO

    val cost = if (prod != null) {
        FinancialCalculator.calculateCost(quantityToSell, BigDecimal.valueOf(prod.purchasePrice))
    } else BigDecimal.ZERO

    val profit = if (prod != null) {
        FinancialCalculator.calculateGrossProfit(revenue, cost)
    } else BigDecimal.ZERO

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. HEADER (Cart Icon + "New Sale" + Blue Plus) ---
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
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "New Sale",
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = "New Sale",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MakiTextDark
                    )
                }

                IconButton(
                    onClick = { showProductPicker = true },
                    modifier = Modifier.testTag("sales_top_add_product")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Select Product",
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- 2. CUSTOMER SELECTOR CARD ---
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomerPicker = true }
                    .testTag("sales_customer_selector")
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MakiLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Customer",
                            tint = MakiPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customer (Optional)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MakiTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selCustomer?.fullName ?: "Select Customer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selCustomer != null) MakiTextDark else MakiTextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Choose Customer",
                        tint = MakiTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 3. SELECTED PRODUCT CARD WITH CIRCULAR STEPPER ---
        item {
            if (prod != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProductPicker = true }
                        .testTag("sales_selected_product_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Product image thumbnail
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            ProductThumbnailArt(
                                productName = prod.name,
                                category = prod.category,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Product Name & details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prod.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MakiTextDark
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${FinancialCalculator.formatBirr(prod.sellingPrice)} • ${prod.stockQuantity} in stock",
                                fontSize = 12.sp,
                                color = MakiTextSecondary
                            )
                        }

                        // Blue Circular Stepper: [-] 2 [+]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MakiPrimaryBlue)
                                    .clickable {
                                        if (quantityToSell > 1) quantityToSell--
                                    }
                                    .testTag("sales_qty_decrease"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "$quantityToSell",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MakiTextDark,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MakiPrimaryBlue)
                                    .clickable {
                                        if (quantityToSell < prod.stockQuantity) {
                                            quantityToSell++
                                        }
                                    }
                                    .testTag("sales_qty_increase"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showProductPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Select Product for Sale")
                }
            }
        }

        // --- 4. FINANCIAL BREAKDOWN ROWS ---
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales_financial_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Revenue",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MakiTextSecondary
                        )
                        Text(
                            text = FinancialCalculator.formatBirr(revenue),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiTextDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Cost",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MakiTextSecondary
                        )
                        Text(
                            text = FinancialCalculator.formatBirr(cost),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiTextDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gross Profit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiTextDark
                        )
                        Text(
                            text = FinancialCalculator.formatBirr(profit),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MakiSuccess
                        )
                    }
                }
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage!!,
                    color = MakiError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // --- 5. FULL-WIDTH BLUE BUTTON [✓ Confirm Sale] ---
        item {
            Button(
                onClick = {
                    if (prod == null) {
                        errorMessage = "Please select a product"
                        return@Button
                    }
                    if (quantityToSell <= 0 || quantityToSell > prod.stockQuantity) {
                        errorMessage = "Invalid quantity or insufficient stock"
                        return@Button
                    }
                    errorMessage = null
                    isSubmitting = true
                    viewModel.recordSale(
                        productId = prod.id,
                        quantity = quantityToSell,
                        customerId = selectedCustomerId,
                        notes = "",
                        onSuccess = { sale ->
                            isSubmitting = false
                            completedSaleReceipt = sale
                            quantityToSell = 1
                        },
                        onError = { err ->
                            isSubmitting = false
                            errorMessage = err
                        }
                    )
                },
                enabled = !isSubmitting && prod != null && prod.stockQuantity > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sales_confirm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Sale",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // --- 6. INFO CARD: "You will earn X ETB profit from this sale." ---
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MakiLightBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sales_info_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MakiPrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "You will earn ${FinancialCalculator.formatBirr(profit)} profit from this sale.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E40AF)
                    )
                }
            }
        }

        // --- 7. RECENT SALES SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Sales",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MakiTextDark
                    )

                    TextButton(onClick = onNavigateToHistory) {
                        Text(
                            text = "See All →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiPrimaryBlue
                        )
                    }
                }

                val recentList = allSales.take(3)
                if (recentList.isEmpty()) {
                    Text(
                        text = "No sales recorded yet.",
                        fontSize = 13.sp,
                        color = MakiTextTertiary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    recentList.forEach { sale ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    ProductThumbnailArt(
                                        productName = sale.productNameSnapshot,
                                        category = "Gift",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sale.productNameSnapshot,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MakiTextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${FinancialCalculator.formatBirr(sale.totalRevenue)} • ${sale.quantity} pc",
                                        fontSize = 12.sp,
                                        color = MakiTextSecondary
                                    )
                                }

                                val timeStr = try {
                                    val sdf = SimpleDateFormat("HH:mm", Locale.US)
                                    sdf.format(Date(sale.saleTimestamp))
                                } catch (_: Exception) {
                                    "09:42"
                                }
                                Text(
                                    text = "Today $timeStr",
                                    fontSize = 12.sp,
                                    color = MakiTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // --- CUSTOMER PICKER DIALOG ---
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Select Customer", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedCustomerId = null
                                showCustomerPicker = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("No customer (Walk-in)", color = MakiPrimaryBlue, fontWeight = FontWeight.Medium)
                    }

                    customers.forEach { cust ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCustomerId = cust.id
                                    showCustomerPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MakiLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cust.fullName.take(1).uppercase(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MakiPrimaryBlue
                                )
                            }
                            Column {
                                Text(cust.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(cust.phoneNumber, fontSize = 12.sp, color = MakiTextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text("Close") }
            },
            containerColor = Color.White
        )
    }

    // --- PRODUCT PICKER DIALOG ---
    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Choose Product", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeProducts.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedProduct = item
                                    quantityToSell = 1
                                    showProductPicker = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.size(36.dp)) {
                                ProductThumbnailArt(
                                    productName = item.name,
                                    category = item.category,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "${FinancialCalculator.formatBirr(item.sellingPrice)} • Stock: ${item.stockQuantity}",
                                    fontSize = 12.sp,
                                    color = MakiTextSecondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProductPicker = false }) { Text("Close") }
            },
            containerColor = Color.White
        )
    }

    // --- COMPLETED SALE RECEIPT DIALOG ---
    completedSaleReceipt?.let { sale ->
        val recNumber = "REC-${sale.id.toString().padStart(5, '0')}"
        AlertDialog(
            onDismissRequest = { completedSaleReceipt = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MakiSuccess,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text("Sale Successful!", fontWeight = FontWeight.Bold, color = MakiTextDark)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Receipt #: $recNumber", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Product: ${sale.productNameSnapshot}", fontSize = 14.sp)
                    Text("Quantity: ${sale.quantity}", fontSize = 14.sp)
                    Text("Total Revenue: ${FinancialCalculator.formatBirr(sale.totalRevenue)}", fontWeight = FontWeight.Bold)
                    Text(
                        "Profit Earned: ${FinancialCalculator.formatBirr(sale.grossProfit)}",
                        fontWeight = FontWeight.Bold,
                        color = MakiSuccess
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "MAKI BRANDS\nReceipt: $recNumber\nItem: ${sale.productNameSnapshot} x${sale.quantity}\nTotal: ${FinancialCalculator.formatBirr(sale.totalRevenue)} ETB\nThank you for shopping with us!"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { completedSaleReceipt = null }) {
                    Text("Done", color = MakiTextSecondary)
                }
            },
            containerColor = Color.White
        )
    }
}
