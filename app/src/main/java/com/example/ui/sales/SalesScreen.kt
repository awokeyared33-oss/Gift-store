package com.example.ui.sales

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.domain.calculator.FinancialCalculator
import com.example.localization.AppLanguage
import com.example.ui.components.ProductThumbnailArt
import com.example.ui.theme.MakiCardBorder
import com.example.ui.theme.MakiCharcoal
import com.example.ui.theme.MakiCharcoalLight
import com.example.ui.theme.MakiCharcoalMuted
import com.example.ui.theme.MakiChampagneGold
import com.example.ui.theme.MakiEmerald
import com.example.ui.theme.MakiEmeraldContainer
import com.example.ui.theme.MakiEmeraldDark
import com.example.ui.theme.MakiError
import com.example.ui.theme.MakiGoldLight
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceWhite
import com.example.ui.theme.MakiWarmIvory
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
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAmharic = (currentLang == AppLanguage.AMHARIC)

    val activeProducts by viewModel.activeProducts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    // --- POS State ---
    // Mapping of Product ID to selected quantity in current cart
    val cartQuantities = remember { mutableStateMapOf<Long, Int>() }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Dialog & Checkout states
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var completedSalesReceipt by remember { mutableStateOf<List<SaleEntity>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Derived distinct categories from active catalog
    val categories = remember(activeProducts) {
        activeProducts.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    // Filtered products based on search query and category chip
    val filteredProducts = remember(activeProducts, searchQuery, selectedCategory) {
        activeProducts.filter { product ->
            val matchesCategory = selectedCategory == null || product.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true) ||
                    product.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Selected items computed statistics
    val selectedProductEntities = remember(cartQuantities.keys, activeProducts) {
        cartQuantities.mapNotNull { (prodId, qty) ->
            val p = activeProducts.firstOrNull { it.id == prodId }
            if (p != null) p to qty else null
        }
    }

    val totalItemCount = cartQuantities.size
    val totalUnitsCount = cartQuantities.values.sum()
    val totalSaleAmount = remember(selectedProductEntities) {
        selectedProductEntities.fold(BigDecimal.ZERO) { acc, (prod, qty) ->
            acc.add(BigDecimal.valueOf(prod.sellingPrice).multiply(BigDecimal.valueOf(qty.toLong())))
        }
    }

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MakiWarmIvory)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. COMPACT TOP POS HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MakiSurfaceWhite)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAmharic) "አዲስ ሽያጭ" else "New Sale",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MakiCharcoal,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isAmharic) "ለመሸጥ እቃዎችን ይምረጡ" else "Select products to sell.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = MakiCharcoalLight
                    )
                }

                // Quick Sales History shortcut button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MakiEmeraldContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MakiCardBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToHistory() }
                        .testTag("pos_history_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MakiEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAmharic) "ታሪክ" else "History",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiEmerald
                        )
                    }
                }
            }

            // --- 2. SEARCH & CATEGORY CHIPS BAR ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MakiSurfaceWhite)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isAmharic) "እቃዎችን በስም ወይም በምድብ ፈልግ..." else "Search products by name...",
                            fontSize = 13.sp,
                            color = MakiCharcoalMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MakiEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MakiCharcoalMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MakiEmerald,
                        unfocusedBorderColor = MakiCardBorder,
                        focusedContainerColor = MakiWarmIvory,
                        unfocusedContainerColor = MakiWarmIvory
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pos_search_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(if (isAmharic) "ሁሉም" else "All", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MakiEmerald,
                                selectedLabelColor = Color.White,
                                containerColor = MakiWarmIvory,
                                labelColor = MakiCharcoalLight
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == null,
                                borderColor = if (selectedCategory == null) MakiEmerald else MakiCardBorder
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    items(categories) { categoryName ->
                        FilterChip(
                            selected = selectedCategory.equals(categoryName, ignoreCase = true),
                            onClick = {
                                selectedCategory = if (selectedCategory.equals(categoryName, ignoreCase = true)) null else categoryName
                            },
                            label = { Text(categoryName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MakiEmerald,
                                selectedLabelColor = Color.White,
                                containerColor = MakiWarmIvory,
                                labelColor = MakiCharcoalLight
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory.equals(categoryName, ignoreCase = true),
                                borderColor = if (selectedCategory.equals(categoryName, ignoreCase = true)) MakiEmerald else MakiCardBorder
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // Divider border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MakiCardBorder)
            )

            // --- 3. TWO-COLUMN PRODUCT GRID ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("pos_product_grid")
            ) {
                if (filteredProducts.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MakiEmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = MakiEmerald,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isAmharic) "ምንም እቃ አልተገኘም" else "No matching products found",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MakiCharcoal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAmharic) "የፍለጋ ቃሉን ወይም ምድቡን ይቀይሩ" else "Try clearing your search query or category filter.",
                                fontSize = 12.sp,
                                color = MakiCharcoalLight
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = null
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isAmharic) "ማጣሪያውን አጽዳ" else "Reset Filters", color = MakiEmerald)
                            }
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { product ->
                        val selectedQty = cartQuantities[product.id] ?: 0
                        val isSelected = selectedQty > 0
                        val isOutOfStock = product.stockQuantity <= 0

                        ProductGridCard(
                            product = product,
                            selectedQuantity = selectedQty,
                            isSelected = isSelected,
                            isOutOfStock = isOutOfStock,
                            isAmharic = isAmharic,
                            onAdd = {
                                if (!isOutOfStock) {
                                    val current = cartQuantities[product.id] ?: 0
                                    if (current < product.stockQuantity) {
                                        cartQuantities[product.id] = current + 1
                                    }
                                }
                            },
                            onRemove = {
                                val current = cartQuantities[product.id] ?: 0
                                if (current > 1) {
                                    cartQuantities[product.id] = current - 1
                                } else {
                                    cartQuantities.remove(product.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- 4. FIXED BOTTOM CHECKOUT PANEL ---
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MakiSurfaceWhite,
            shadowElevation = 14.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MakiCardBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .testTag("pos_checkout_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Optional Customer Selector Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedCustomer != null) MakiEmeraldContainer else MakiWarmIvory,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedCustomer != null) MakiEmerald else MakiCardBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showCustomerPicker = true }
                            .testTag("pos_customer_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Customer",
                                tint = if (selectedCustomer != null) MakiEmerald else MakiCharcoalLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = selectedCustomer?.fullName ?: if (isAmharic) "ደንበኛ: ተራ ደንበኛ (አማራጭ)" else "Customer: Walk-in (Optional)",
                                fontSize = 11.sp,
                                fontWeight = if (selectedCustomer != null) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedCustomer != null) MakiEmerald else MakiCharcoalLight
                            )
                        }
                    }

                    if (selectedCustomer != null) {
                        IconButton(
                            onClick = { selectedCustomerId = null },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Customer",
                                tint = MakiCharcoalMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cart Summary Row: (Items count, Total units, Total ETB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isAmharic)
                                "የተመረጡ: $totalItemCount | ብዛት: $totalUnitsCount"
                            else
                                "Selected items: $totalItemCount • Total quantity: $totalUnitsCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MakiCharcoalLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (isAmharic) "ድምር: " else "Total: "}${FinancialCalculator.formatBirr(totalSaleAmount)}",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MakiCharcoal
                        )
                    }

                    // Prominent CONFIRM SALE Button
                    Button(
                        onClick = {
                            if (cartQuantities.isNotEmpty()) {
                                showConfirmDialog = true
                            }
                        },
                        enabled = cartQuantities.isNotEmpty() && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MakiEmerald,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFD1D8D4),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sales_confirm_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MakiChampagneGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isAmharic) "ሽያጭ አረጋግጥ" else "CONFIRM SALE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 5. OPTIONAL CUSTOMER SELECTION MODAL ---
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = {
                Text(
                    text = if (isAmharic) "ደንበኛ ይምረጡ" else "Select Customer (Optional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MakiCharcoal
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Walk-in option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedCustomerId == null) MakiEmeraldContainer else MakiWarmIvory,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MakiCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCustomerId = null
                                showCustomerPicker = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAmharic) "ተራ ደንበኛ (ያልተመዘገበ)" else "Walk-in Customer (No Profile)",
                                fontWeight = if (selectedCustomerId == null) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCustomerId == null) MakiEmerald else MakiCharcoal,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Registered Customers List
                    customers.forEach { cust ->
                        val isPicked = selectedCustomerId == cust.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPicked) MakiEmeraldContainer else MakiSurfaceWhite,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPicked) MakiEmerald else MakiCardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCustomerId = cust.id
                                    showCustomerPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = cust.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MakiCharcoal
                                    )
                                    Text(
                                        text = cust.phoneNumber,
                                        fontSize = 11.sp,
                                        color = MakiCharcoalLight
                                    )
                                }
                                if (isPicked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MakiEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text(if (isAmharic) "ዝጋ" else "Close", color = MakiEmerald)
                }
            },
            containerColor = MakiSurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- 6. CONFIRMATION DIALOG (Items breakdown, Stock Validation & Total) ---
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showConfirmDialog = false },
            title = {
                Text(
                    text = if (isAmharic) "ሽያጩን ማረጋገጫ" else "Confirm Sale",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MakiCharcoal
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isAmharic) "የተመረጡ እቃዎች:" else "Review order items:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MakiCharcoalLight
                    )

                    // Line items breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MakiWarmIvory)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedProductEntities.forEach { (prod, qty) ->
                            val lineTotal = BigDecimal.valueOf(prod.sellingPrice).multiply(BigDecimal.valueOf(qty.toLong()))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${qty}x ${prod.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MakiCharcoal,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = FinancialCalculator.formatBirr(lineTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MakiCharcoal
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MakiCardBorder)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isAmharic) "ጠቅላላ ድምር" else "Total Amount",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MakiCharcoal
                            )
                            Text(
                                text = FinancialCalculator.formatBirr(totalSaleAmount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MakiEmerald
                            )
                        }
                    }

                    if (selectedCustomer != null) {
                        Text(
                            text = "${if (isAmharic) "ደንበኛ: " else "Customer: "}${selectedCustomer.fullName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiEmerald
                        )
                    }

                    Text(
                        text = if (isAmharic)
                            "እቃዎቹ ከሱቅ ክምችት ላይ ወዲያውኑ ይቀነሳሉ።"
                        else
                            "Inventory will be atomically deducted and recorded in your store ledger.",
                        fontSize = 11.sp,
                        color = MakiCharcoalLight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        errorMessage = null

                        val itemsToSell = cartQuantities.map { (prodId, qty) -> prodId to qty }

                        viewModel.recordMultiItemSale(
                            items = itemsToSell,
                            customerId = selectedCustomerId,
                            notes = "POS Checkout (${itemsToSell.size} items)",
                            onSuccess = { sales ->
                                isSubmitting = false
                                showConfirmDialog = false
                                cartQuantities.clear()
                                selectedCustomerId = null
                                completedSalesReceipt = sales
                            },
                            onError = { err ->
                                isSubmitting = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MakiEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isAmharic) "አረጋግጥና ቀንስ" else "Confirm & Complete",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isSubmitting) showConfirmDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text(if (isAmharic) "ተመለስ" else "Cancel", color = MakiCharcoalLight)
                }
            },
            containerColor = MakiSurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- 7. SALE SUCCESSFUL RECEIPT DIALOG ---
    completedSalesReceipt?.let { sales ->
        val totalRevenue = sales.sumOf { it.totalRevenue }
        val ethDate = sales.firstOrNull()?.ethiopianDateStr ?: ""
        val receiptId = sales.firstOrNull()?.id ?: 0L

        AlertDialog(
            onDismissRequest = { completedSalesReceipt = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MakiEmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MakiEmerald,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (isAmharic) "ሽያጩ በተሳካ ሁኔታ ተጠናቋል!" else "Sale Completed Successfully!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    color = MakiCharcoal
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isAmharic)
                            "ክምችት ተስተካክሏል። ደረሰኝ ዝርዝር:"
                        else
                            "Stock deducted and recorded. Receipt summary:",
                        fontSize = 12.sp,
                        color = MakiCharcoalLight
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MakiWarmIvory)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sales.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${s.quantity}x ${s.productNameSnapshot}",
                                    fontSize = 12.sp,
                                    color = MakiCharcoal
                                )
                                Text(
                                    text = "${s.totalRevenue} ETB",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MakiCharcoal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MakiCardBorder))
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isAmharic) "ድምር" else "Total Paid",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MakiCharcoal
                            )
                            Text(
                                text = "$totalRevenue ETB",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MakiEmerald
                            )
                        }

                        if (ethDate.isNotBlank()) {
                            Text(
                                text = "Date: $ethDate (E.C.)",
                                fontSize = 11.sp,
                                color = MakiCharcoalMuted
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { completedSalesReceipt = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MakiEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isAmharic) "ተከናውኗል" else "Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val shareText = StringBuilder().apply {
                            append("MAKI BRANDS - Receipt #$receiptId\n")
                            append("Date: $ethDate\n")
                            sales.forEach { s ->
                                append("${s.quantity}x ${s.productNameSnapshot} = ${s.totalRevenue} ETB\n")
                            }
                            append("Total: $totalRevenue ETB\n")
                            append("Thank you for shopping at Maki Brands!")
                        }.toString()

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Receipt"))
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = MakiEmerald)
                        Text(if (isAmharic) "ደረሰኝ አጋራ" else "Share Receipt", color = MakiEmerald)
                    }
                }
            },
            containerColor = MakiSurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Error Alert Dialog if sale submission encounters an issue
    errorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Error",
                    tint = MakiError,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (isAmharic) "ሽያጩ አልተሳካም" else "Sale Error",
                    fontWeight = FontWeight.Bold,
                    color = MakiError
                )
            },
            text = { Text(err, color = MakiCharcoal) },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MakiError)
                ) {
                    Text(if (isAmharic) "እሺ" else "OK", color = Color.White)
                }
            },
            containerColor = MakiSurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Modern 2-Column POS Product Card featuring responsive thumbnail, price,
 * in-stock availability indicator, and stepper controls when selected.
 */
@Composable
private fun ProductGridCard(
    product: ProductEntity,
    selectedQuantity: Int,
    isSelected: Boolean,
    isOutOfStock: Boolean,
    isAmharic: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MakiEmerald
            else -> MakiCardBorder
        },
        label = "cardBorder"
    )

    val containerBg by animateColorAsState(
        targetValue = when {
            isSelected -> MakiEmeraldContainer.copy(alpha = 0.35f)
            else -> MakiSurfaceWhite
        },
        label = "cardBg"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .alpha(if (isOutOfStock) 0.5f else 1f)
            .clickable(enabled = !isOutOfStock) { onAdd() }
            .testTag("pos_product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Product Artwork & Category container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MakiWarmIvory),
                contentAlignment = Alignment.Center
            ) {
                ProductThumbnailArt(
                    productName = product.name,
                    category = product.category,
                    modifier = Modifier.size(105.dp)
                )

                // Stock Badge Pill in Top Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isOutOfStock -> MakiError.copy(alpha = 0.9f)
                                product.stockQuantity <= product.lowStockThreshold -> Color(0xFFD97706)
                                else -> MakiEmeraldDark.copy(alpha = 0.85f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            isOutOfStock -> if (isAmharic) "አልቋል" else "Out of Stock"
                            else -> "${product.stockQuantity} ${if (isAmharic) "አለ" else "left"}"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Selected Checkmark Pill in Top Left
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MakiEmerald),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            Text(
                text = product.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MakiCharcoal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Product Price in ETB
            Text(
                text = "${product.sellingPrice.toInt()} ETB",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MakiEmerald
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Add (+) Button or Stepper Controls (- Qty +)
            if (isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MakiSurfaceWhite)
                        .border(1.dp, MakiEmerald, RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minus",
                            tint = MakiEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$selectedQuantity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MakiEmerald
                    )

                    IconButton(
                        onClick = onAdd,
                        enabled = selectedQuantity < product.stockQuantity,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Plus",
                            tint = if (selectedQuantity < product.stockQuantity) MakiEmerald else MakiCharcoalMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOutOfStock) Color(0xFFE2E8F0) else MakiEmeraldContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isOutOfStock) { onAdd() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = if (isOutOfStock) MakiCharcoalMuted else MakiEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOutOfStock) (if (isAmharic) "አልቋል" else "Unavailable") else (if (isAmharic) "አክል" else "Add"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOutOfStock) MakiCharcoalMuted else MakiEmerald
                        )
                    }
                }
            }
        }
    }
}
