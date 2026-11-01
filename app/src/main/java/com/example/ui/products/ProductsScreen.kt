package com.example.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ProductEntity
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
import com.example.ui.theme.MakiWarning
import com.example.ui.viewmodel.MakiViewModel
import java.math.BigDecimal

@Composable
fun ProductsScreen(
    viewModel: MakiViewModel,
    modifier: Modifier = Modifier
) {
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val products by viewModel.activeProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToRestock by remember { mutableStateOf<ProductEntity?>(null) }
    var productToAdjust by remember { mutableStateOf<ProductEntity?>(null) }
    var showMoreCategoriesDropdown by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true) ||
                    p.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = (selectedCategory == null || p.category.equals(selectedCategory, ignoreCase = true))
            matchesSearch && matchesCategory
        }
    }

    val baseCategories = listOf("All", "Chocolate", "Gift Box", "Flowers")
    val moreCategories = remember(categories) {
        categories.map { it.name }.filter { cat ->
            baseCategories.none { it.equals(cat, ignoreCase = true) }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = MakiPrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("products_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = Color.White,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // --- 1. HEADER (Menu + Products title + Blue Plus Icon) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Navigation drawer if applicable */ }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MakiTextDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = strings.products,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiTextDark
                )

                IconButton(
                    onClick = { showAddProductDialog = true },
                    modifier = Modifier.testTag("products_top_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product",
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- 2. SEARCH BAR WITH FILTER FUNNEL ICON ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MakiTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search products...",
                                fontSize = 14.sp,
                                color = MakiTextTertiary
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = MakiTextDark,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("products_search_input")
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = MakiPrimaryBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                // Clear or reset category filter
                                selectedCategory = null
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 3. CATEGORY CHIPS ROW (All, Chocolate, Gift Box, Flowers, More ▾) ---
            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                baseCategories.forEach { categoryLabel ->
                    val isSelected = if (categoryLabel == "All") {
                        selectedCategory == null
                    } else {
                        selectedCategory.equals(categoryLabel, ignoreCase = true)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MakiPrimaryBlue else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MakiPrimaryBlue else MakiSurfaceBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedCategory = if (categoryLabel == "All") null else categoryLabel
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = categoryLabel,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MakiTextDark
                        )
                    }
                }

                // "More ▾" chip
                Box {
                    val isMoreSelected = selectedCategory != null &&
                            baseCategories.none { it.equals(selectedCategory, ignoreCase = true) }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isMoreSelected) MakiPrimaryBlue else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isMoreSelected) MakiPrimaryBlue else MakiSurfaceBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { showMoreCategoriesDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMoreSelected) (selectedCategory ?: "More") else "More",
                            fontSize = 13.sp,
                            fontWeight = if (isMoreSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isMoreSelected) Color.White else MakiTextDark
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "More categories",
                            tint = if (isMoreSelected) Color.White else MakiTextDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreCategoriesDropdown,
                        onDismissRequest = { showMoreCategoriesDropdown = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        if (moreCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Cards & Decor") },
                                onClick = {
                                    selectedCategory = "Cards"
                                    showMoreCategoriesDropdown = false
                                }
                            )
                        } else {
                            moreCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        showMoreCategoriesDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 4. 2-COLUMN PRODUCT GRID ---
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MakiTextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No products found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MakiTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAddProductDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Add First Product")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onEdit = { productToEdit = product },
                            onRestock = { productToRestock = product },
                            onAdjust = { productToAdjust = product },
                            onArchive = { viewModel.archiveProduct(product.id) {} }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS (Add Product, Edit, Restock, Adjust) ---
    if (showAddProductDialog) {
        ProductFormDialog(
            strings = strings,
            existingProduct = null,
            categories = categories.map { it.name },
            onDismiss = { showAddProductDialog = false },
            onSave = { name, category, cost, selling, stock, minStock, desc ->
                viewModel.saveProduct(
                    name = name,
                    category = category,
                    purchasePrice = cost,
                    sellingPrice = selling,
                    stockQuantity = stock,
                    lowStockThreshold = minStock,
                    description = desc
                ) {
                    showAddProductDialog = false
                }
            }
        )
    }

    productToEdit?.let { prod ->
        ProductFormDialog(
            strings = strings,
            existingProduct = prod,
            categories = categories.map { it.name },
            onDismiss = { productToEdit = null },
            onSave = { name, category, cost, selling, stock, minStock, desc ->
                viewModel.saveProduct(
                    id = prod.id,
                    name = name,
                    category = category,
                    purchasePrice = cost,
                    sellingPrice = selling,
                    stockQuantity = prod.stockQuantity,
                    lowStockThreshold = minStock,
                    description = desc
                ) {
                    productToEdit = null
                }
            }
        )
    }

    productToRestock?.let { prod ->
        RestockDialog(
            product = prod,
            onDismiss = { productToRestock = null },
            onConfirm = { quantity, reason ->
                viewModel.restockProduct(
                    productId = prod.id,
                    quantityToAdd = quantity,
                    reason = reason
                ) {
                    productToRestock = null
                }
            }
        )
    }

    productToAdjust?.let { prod ->
        StockAdjustmentDialog(
            product = prod,
            onDismiss = { productToAdjust = null },
            onConfirm = { newStock, reason ->
                val delta = newStock - prod.stockQuantity
                viewModel.adjustInventory(
                    productId = prod.id,
                    delta = delta,
                    isDamage = false,
                    reason = reason
                ) {
                    productToAdjust = null
                }
            }
        )
    }
}

/**
 * Clean Product Card matching the 2-column layout in the reference mockup.
 */
@Composable
fun ProductCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onAdjust: () -> Unit,
    onArchive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column {
            // Product Visual Container with 3-dot menu in top-right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color(0xFFF8FAFC))
            ) {
                ProductThumbnailArt(
                    productName = product.name,
                    category = product.category,
                    modifier = Modifier.fillMaxSize()
                )

                // 3-dot menu button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MakiTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Product") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MakiPrimaryBlue) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restock") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MakiSuccess) },
                            onClick = {
                                showMenu = false
                                onRestock()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Adjust Stock") },
                            leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MakiWarning) },
                            onClick = {
                                showMenu = false
                                onAdjust()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive", color = MakiError) },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = MakiError) },
                            onClick = {
                                showMenu = false
                                onArchive()
                            }
                        )
                    }
                }
            }

            // Product Details below image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiTextDark,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = FinancialCalculator.formatBirr(product.sellingPrice),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiTextDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stock Badge Pill
                val isLow = product.stockQuantity <= product.lowStockThreshold
                val isOut = product.stockQuantity <= 0
                val (badgeBg, badgeText) = when {
                    isOut -> Color(0xFFFEE2E2) to MakiError
                    isLow -> Color(0xFFFEF3C7) to MakiWarning
                    else -> Color(0xFFDCFCE7) to MakiSuccess
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = badgeText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${product.stockQuantity} in stock",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeText
                    )
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    strings: com.example.localization.AppStrings,
    existingProduct: ProductEntity?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stock: Int,
        minStock: Int,
        description: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var category by remember { mutableStateOf(existingProduct?.category ?: (categories.firstOrNull() ?: "Chocolate")) }
    var costPriceStr by remember { mutableStateOf(existingProduct?.purchasePrice?.toString() ?: "") }
    var sellingPriceStr by remember { mutableStateOf(existingProduct?.sellingPrice?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(existingProduct?.stockQuantity?.toString() ?: "10") }
    var minStockStr by remember { mutableStateOf(existingProduct?.lowStockThreshold?.toString() ?: "3") }
    var description by remember { mutableStateOf(existingProduct?.description ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingProduct == null) "Add Product" else "Edit Product",
                fontWeight = FontWeight.Bold,
                color = MakiTextDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MakiError, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Chocolate, Gift Box, Flowers) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Cost (ETB) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("Price (ETB) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (existingProduct == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Initial Stock *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minStockStr,
                            onValueChange = { minStockStr = it },
                            label = { Text("Min Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPriceStr.toDoubleOrNull()
                    val sell = sellingPriceStr.toDoubleOrNull()
                    val stock = stockStr.toIntOrNull() ?: 0
                    val minStock = minStockStr.toIntOrNull() ?: 3

                    if (name.isBlank() || category.isBlank() || cost == null || sell == null) {
                        errorMsg = "Please fill in all required fields"
                        return@Button
                    }
                    onSave(name.trim(), category.trim(), cost, sell, stock, minStock, description.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MakiTextSecondary)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun RestockDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, reason: String) -> Unit
) {
    var quantityStr by remember { mutableStateOf("10") }
    var reason by remember { mutableStateOf("Supplier restock") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock: ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity Added *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Restock Reason") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toIntOrNull() ?: 0
                    if (qty > 0) {
                        onConfirm(qty, reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Restock")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White
    )
}

@Composable
fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (newCount: Int, reason: String) -> Unit
) {
    var newCountStr by remember { mutableStateOf(product.stockQuantity.toString()) }
    var reason by remember { mutableStateOf("Physical inventory count") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Stock: ${product.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newCountStr,
                    onValueChange = { newCountStr = it },
                    label = { Text("Actual Stock Count *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for adjustment *") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = newCountStr.toIntOrNull()
                    if (count != null && count >= 0) {
                        onConfirm(count, reason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Update Stock")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White
    )
}
