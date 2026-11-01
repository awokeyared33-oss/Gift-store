package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.MakiDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CelebrationEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.domain.calculator.FinancialCalculator
import com.example.domain.calendar.EthiopianCalendarConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

class MakiRepository(
    private val db: MakiDatabase,
    private val context: android.content.Context? = null
) {

    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val activeProducts: Flow<List<ProductEntity>> = db.productDao().getActiveProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    val allSales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    val allMovements: Flow<List<InventoryMovementEntity>> = db.inventoryMovementDao().getAllMovements()
    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val activeCustomers: Flow<List<CustomerEntity>> = db.customerDao().getActiveCustomers()
    val allCelebrations: Flow<List<CelebrationEntity>> = db.celebrationDao().getAllCelebrations()
    val activeCelebrations: Flow<List<CelebrationEntity>> = db.celebrationDao().getActiveCelebrations()
    val allExpenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()

    fun getSalesForCustomer(customerId: Long): Flow<List<SaleEntity>> =
        db.saleDao().getSalesForCustomer(customerId)

    fun getCelebrationsForCustomer(customerId: Long): Flow<List<CelebrationEntity>> =
        db.celebrationDao().getCelebrationsForCustomer(customerId)

    fun getSettingFlow(key: String): Flow<AppSettingsEntity?> =
        db.appSettingsDao().getSettingFlow(key)

    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) {
        db.appSettingsDao().getSetting(key)?.value
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        db.appSettingsDao().setSetting(AppSettingsEntity(key, value))
    }

    // --- Product Operations ---
    suspend fun saveProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            val id = db.productDao().insert(product)
            // Log initial stock creation if > 0
            if (product.stockQuantity > 0) {
                val ethDateStr = EthiopianCalendarConverter.toEthiopian(LocalDate.now()).format()
                db.inventoryMovementDao().insert(
                    InventoryMovementEntity(
                        productId = id,
                        productNameSnapshot = product.name,
                        movementType = "INITIAL_STOCK",
                        quantityDelta = product.stockQuantity,
                        previousQuantity = 0,
                        newQuantity = product.stockQuantity,
                        reason = "Initial inventory setup",
                        ethiopianDateStr = ethDateStr
                    )
                )
            }
            id
        } else {
            db.productDao().update(product)
            product.id
        }
    }

    suspend fun archiveProduct(id: Long) = withContext(Dispatchers.IO) {
        db.productDao().archiveProduct(id)
    }

    suspend fun restockProduct(productId: Long, quantityToAdd: Int, reason: String = "Stock Purchase") =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val product = db.productDao().getProductById(productId)
                    ?: throw IllegalArgumentException("Product not found")
                val prev = product.stockQuantity
                val newQty = prev + quantityToAdd
                db.productDao().updateStock(productId, newQty)

                val ethDateStr = EthiopianCalendarConverter.toEthiopian(LocalDate.now()).format()
                db.inventoryMovementDao().insert(
                    InventoryMovementEntity(
                        productId = productId,
                        productNameSnapshot = product.name,
                        movementType = "PURCHASE_RESTOCK",
                        quantityDelta = quantityToAdd,
                        previousQuantity = prev,
                        newQuantity = newQty,
                        reason = reason,
                        ethiopianDateStr = ethDateStr
                    )
                )
            }
        }

    suspend fun adjustInventory(
        productId: Long,
        quantityDelta: Int,
        isDamage: Boolean,
        reason: String
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val product = db.productDao().getProductById(productId)
                ?: throw IllegalArgumentException("Product not found")
            val prev = product.stockQuantity
            val newQty = (prev + quantityDelta).coerceAtLeast(0)
            db.productDao().updateStock(productId, newQty)

            val ethDateStr = EthiopianCalendarConverter.toEthiopian(LocalDate.now()).format()
            val type = if (isDamage) "DAMAGE_LOSS" else "MANUAL_ADJUSTMENT"
            db.inventoryMovementDao().insert(
                InventoryMovementEntity(
                    productId = productId,
                    productNameSnapshot = product.name,
                    movementType = type,
                    quantityDelta = quantityDelta,
                    previousQuantity = prev,
                    newQuantity = newQty,
                    reason = reason,
                    ethiopianDateStr = ethDateStr
                )
            )
        }
    }

    // --- Individual Product Sale ---
    suspend fun recordSale(
        productId: Long,
        quantity: Int,
        customerId: Long? = null,
        notes: String = ""
    ): SaleEntity = withContext(Dispatchers.IO) {
        db.withTransaction {
            val product = db.productDao().getProductById(productId)
                ?: throw IllegalArgumentException("Product not found")

            if (quantity <= 0) {
                throw IllegalArgumentException("Quantity must be greater than zero")
            }
            if (product.stockQuantity < quantity) {
                throw IllegalStateException("Insufficient stock. Available: ${product.stockQuantity}, Requested: $quantity")
            }

            // Financial calculations using BigDecimal
            val financials = FinancialCalculator.calculateSaleFinancials(
                quantity = quantity,
                purchasePrice = BigDecimal.valueOf(product.purchasePrice),
                sellingPrice = BigDecimal.valueOf(product.sellingPrice)
            )

            // Deduct inventory
            val prevStock = product.stockQuantity
            val newStock = prevStock - quantity
            db.productDao().updateStock(productId, newStock)

            val ethDateStr = EthiopianCalendarConverter.toEthiopian(LocalDate.now()).format()

            var customerName: String? = null
            if (customerId != null && customerId > 0) {
                val customer = db.customerDao().getCustomerById(customerId)
                customerName = customer?.fullName
            }

            // Save sale record with snapshot
            val sale = SaleEntity(
                productId = productId,
                productNameSnapshot = product.name,
                quantity = quantity,
                purchasePriceSnapshot = financials.purchasePrice.toDouble(),
                sellingPriceSnapshot = financials.sellingPrice.toDouble(),
                totalRevenue = financials.revenue.toDouble(),
                totalCost = financials.cost.toDouble(),
                grossProfit = financials.grossProfit.toDouble(),
                saleTimestamp = System.currentTimeMillis(),
                ethiopianDateStr = ethDateStr,
                customerId = customerId,
                customerNameSnapshot = customerName,
                notes = notes
            )
            val saleId = db.saleDao().insert(sale)

            // Record inventory movement
            db.inventoryMovementDao().insert(
                InventoryMovementEntity(
                    productId = productId,
                    productNameSnapshot = product.name,
                    movementType = "SALE_DEDUCTION",
                    quantityDelta = -quantity,
                    previousQuantity = prevStock,
                    newQuantity = newStock,
                    reason = "Sale #$saleId ($quantity x ${product.name})",
                    ethiopianDateStr = ethDateStr
                )
            )

            // Record into previous gifts history for CRM retention
            if (customerId != null && customerId > 0) {
                db.previousGiftDao().insert(
                    com.example.data.local.entity.PreviousGiftEntity(
                        customerId = customerId,
                        giftItemName = product.name,
                        purchaseDateGregorian = LocalDate.now().toString(),
                        purchaseDateEthiopian = ethDateStr,
                        quantity = quantity,
                        purchasePrice = financials.purchasePrice.toDouble(),
                        sellingPrice = financials.sellingPrice.toDouble(),
                        customizationDetails = notes,
                        recipient = customerName ?: ""
                    )
                )
            }

            sale.copy(id = saleId)
        }
    }

    // --- Void Sale with Inventory Restore ---
    suspend fun voidSale(saleId: Long, reason: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val sale = db.saleDao().getSaleById(saleId)
                ?: throw IllegalArgumentException("Sale not found")
            if (sale.isVoided) {
                throw IllegalStateException("Sale has already been voided")
            }

            // Mark void
            val now = System.currentTimeMillis()
            db.saleDao().voidSale(saleId, reason, now)

            // Restore product inventory
            val product = db.productDao().getProductById(sale.productId)
            if (product != null) {
                val prev = product.stockQuantity
                val restored = prev + sale.quantity
                db.productDao().updateStock(sale.productId, restored)

                val ethDateStr = EthiopianCalendarConverter.toEthiopian(LocalDate.now()).format()
                db.inventoryMovementDao().insert(
                    InventoryMovementEntity(
                        productId = sale.productId,
                        productNameSnapshot = sale.productNameSnapshot,
                        movementType = "SALE_VOID_RESTORE",
                        quantityDelta = sale.quantity,
                        previousQuantity = prev,
                        newQuantity = restored,
                        reason = "Void Sale #$saleId: $reason",
                        timestamp = now,
                        ethiopianDateStr = ethDateStr
                    )
                )
            }
        }
    }

    // --- Customer & CRM ---
    suspend fun archiveCustomer(id: Long) = withContext(Dispatchers.IO) {
        db.customerDao().archiveCustomer(id)
    }

    suspend fun saveCustomer(customer: CustomerEntity): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            db.customerDao().insert(customer)
        } else {
            db.customerDao().update(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        db.customerDao().delete(customer)
    }

    suspend fun saveCelebration(celebration: CelebrationEntity): Long = withContext(Dispatchers.IO) {
        if (celebration.id == 0L) {
            db.celebrationDao().insert(celebration)
        } else {
            db.celebrationDao().update(celebration)
            celebration.id
        }
    }

    suspend fun deleteCelebration(celebration: CelebrationEntity) = withContext(Dispatchers.IO) {
        db.celebrationDao().delete(celebration)
    }

    suspend fun markCelebrationNotified(celebrationId: Long) = withContext(Dispatchers.IO) {
        db.celebrationDao().updateLastNotified(celebrationId, System.currentTimeMillis())
    }

    // --- Categories ---
    suspend fun addCategory(name: String, iconName: String = "gift"): Long = withContext(Dispatchers.IO) {
        db.categoryDao().insert(CategoryEntity(name = name, iconName = iconName))
    }

    // --- Backup & Restore ---
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("schema_version", 2)
        root.put("app_name", "MAKI BRANDS")
        root.put("app_version", "1.0.0")
        root.put("export_timestamp", System.currentTimeMillis())

        // Products
        val products = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM products").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                obj.put("category", cursor.getString(cursor.getColumnIndexOrThrow("category")))
                obj.put("purchasePrice", cursor.getDouble(cursor.getColumnIndexOrThrow("purchasePrice")))
                obj.put("sellingPrice", cursor.getDouble(cursor.getColumnIndexOrThrow("sellingPrice")))
                obj.put("stockQuantity", cursor.getInt(cursor.getColumnIndexOrThrow("stockQuantity")))
                obj.put("lowStockThreshold", cursor.getInt(cursor.getColumnIndexOrThrow("lowStockThreshold")))
                obj.put("description", cursor.getString(cursor.getColumnIndexOrThrow("description")))
                obj.put("isArchived", cursor.getInt(cursor.getColumnIndexOrThrow("isArchived")) == 1)
                obj.put("createdAt", cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
                products.put(obj)
            }
        }
        root.put("products", products)

        // Sales
        val sales = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM sales").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("productId", cursor.getLong(cursor.getColumnIndexOrThrow("productId")))
                obj.put("productNameSnapshot", cursor.getString(cursor.getColumnIndexOrThrow("productNameSnapshot")))
                obj.put("quantity", cursor.getInt(cursor.getColumnIndexOrThrow("quantity")))
                obj.put("purchasePriceSnapshot", cursor.getDouble(cursor.getColumnIndexOrThrow("purchasePriceSnapshot")))
                obj.put("sellingPriceSnapshot", cursor.getDouble(cursor.getColumnIndexOrThrow("sellingPriceSnapshot")))
                obj.put("totalRevenue", cursor.getDouble(cursor.getColumnIndexOrThrow("totalRevenue")))
                obj.put("totalCost", cursor.getDouble(cursor.getColumnIndexOrThrow("totalCost")))
                obj.put("grossProfit", cursor.getDouble(cursor.getColumnIndexOrThrow("grossProfit")))
                obj.put("saleTimestamp", cursor.getLong(cursor.getColumnIndexOrThrow("saleTimestamp")))
                obj.put("ethiopianDateStr", cursor.getString(cursor.getColumnIndexOrThrow("ethiopianDateStr")))
                val custIdx = cursor.getColumnIndexOrThrow("customerId")
                if (!cursor.isNull(custIdx)) obj.put("customerId", cursor.getLong(custIdx))
                val custNameIdx = cursor.getColumnIndexOrThrow("customerNameSnapshot")
                if (!cursor.isNull(custNameIdx)) obj.put("customerNameSnapshot", cursor.getString(custNameIdx))
                obj.put("notes", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
                obj.put("isVoided", cursor.getInt(cursor.getColumnIndexOrThrow("isVoided")) == 1)
                obj.put("voidReason", cursor.getString(cursor.getColumnIndexOrThrow("voidReason")))
                val voidTIdx = cursor.getColumnIndexOrThrow("voidTimestamp")
                if (!cursor.isNull(voidTIdx)) obj.put("voidTimestamp", cursor.getLong(voidTIdx))
                sales.put(obj)
            }
        }
        root.put("sales", sales)

        // Customers
        val customers = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM customers").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("fullName", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
                obj.put("phoneNumber", cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")))
                obj.put("alternativePhone", cursor.getString(cursor.getColumnIndexOrThrow("alternativePhone")))
                val photoCol = cursor.getColumnIndex("photoUri")
                val uriStr = if (photoCol >= 0 && !cursor.isNull(photoCol)) cursor.getString(photoCol) else ""
                obj.put("photoUri", uriStr)
                if (uriStr.isNotBlank()) {
                    try {
                        val file = java.io.File(uriStr)
                        if (file.exists() && file.length() < 2 * 1024 * 1024) {
                            val b64 = android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
                            obj.put("photoBase64", b64)
                        } else if (context != null && uriStr.startsWith("content://")) {
                            context.contentResolver.openInputStream(android.net.Uri.parse(uriStr))?.use { input ->
                                val bytes = input.readBytes()
                                if (bytes.size < 2 * 1024 * 1024) {
                                    val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                    obj.put("photoBase64", b64)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore file read error
                    }
                }
                obj.put("gender", cursor.getString(cursor.getColumnIndexOrThrow("gender")))
                obj.put("address", cursor.getString(cursor.getColumnIndexOrThrow("address")))
                obj.put("city", cursor.getString(cursor.getColumnIndexOrThrow("city")))
                obj.put("notes", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
                obj.put("giftPreferences", cursor.getString(cursor.getColumnIndexOrThrow("giftPreferences")))
                obj.put("preferredLanguage", cursor.getString(cursor.getColumnIndexOrThrow("preferredLanguage")))
                obj.put("preferredContactMethod", cursor.getString(cursor.getColumnIndexOrThrow("preferredContactMethod")))
                obj.put("marketingConsent", cursor.getInt(cursor.getColumnIndexOrThrow("marketingConsent")) == 1)
                val isArchCol = cursor.getColumnIndex("isArchived")
                if (isArchCol >= 0) {
                    obj.put("isArchived", cursor.getInt(isArchCol) == 1)
                } else {
                    obj.put("isArchived", false)
                }
                obj.put("createdAt", cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
                customers.put(obj)
            }
        }
        root.put("customers", customers)

        // Celebrations
        val celebrations = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM celebrations").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("customerId", cursor.getLong(cursor.getColumnIndexOrThrow("customerId")))
                obj.put("celebrationName", cursor.getString(cursor.getColumnIndexOrThrow("celebrationName")))
                obj.put("celebrationCategory", cursor.getString(cursor.getColumnIndexOrThrow("celebrationCategory")))
                obj.put("personCelebrated", cursor.getString(cursor.getColumnIndexOrThrow("personCelebrated")))
                obj.put("relationship", cursor.getString(cursor.getColumnIndexOrThrow("relationship")))
                obj.put("ethMonth", cursor.getInt(cursor.getColumnIndexOrThrow("ethMonth")))
                obj.put("ethDay", cursor.getInt(cursor.getColumnIndexOrThrow("ethDay")))
                obj.put("recurrenceCycle", cursor.getString(cursor.getColumnIndexOrThrow("recurrenceCycle")))
                obj.put("reminderDaysBefore", cursor.getInt(cursor.getColumnIndexOrThrow("reminderDaysBefore")))
                obj.put("notes", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
                obj.put("isActive", cursor.getInt(cursor.getColumnIndexOrThrow("isActive")) == 1)
                val notIdx = cursor.getColumnIndexOrThrow("lastNotifiedTimestamp")
                if (!cursor.isNull(notIdx)) obj.put("lastNotifiedTimestamp", cursor.getLong(notIdx))
                celebrations.put(obj)
            }
        }
        root.put("celebrations", celebrations)

        // Categories
        val categories = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM categories").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                obj.put("iconName", cursor.getString(cursor.getColumnIndexOrThrow("iconName")))
                categories.put(obj)
            }
        }
        root.put("categories", categories)

        // Inventory Movements
        val movements = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM inventory_movements").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("productId", cursor.getLong(cursor.getColumnIndexOrThrow("productId")))
                obj.put("productNameSnapshot", cursor.getString(cursor.getColumnIndexOrThrow("productNameSnapshot")))
                obj.put("movementType", cursor.getString(cursor.getColumnIndexOrThrow("movementType")))
                obj.put("quantityDelta", cursor.getInt(cursor.getColumnIndexOrThrow("quantityDelta")))
                obj.put("previousQuantity", cursor.getInt(cursor.getColumnIndexOrThrow("previousQuantity")))
                obj.put("newQuantity", cursor.getInt(cursor.getColumnIndexOrThrow("newQuantity")))
                obj.put("reason", cursor.getString(cursor.getColumnIndexOrThrow("reason")))
                obj.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
                obj.put("ethiopianDateStr", cursor.getString(cursor.getColumnIndexOrThrow("ethiopianDateStr")))
                movements.put(obj)
            }
        }
        root.put("inventory_movements", movements)

        // Previous Gifts
        val previousGifts = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM previous_gifts").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("customerId", cursor.getLong(cursor.getColumnIndexOrThrow("customerId")))
                obj.put("giftItemName", cursor.getString(cursor.getColumnIndexOrThrow("giftItemName")))
                obj.put("purchaseDateGregorian", cursor.getString(cursor.getColumnIndexOrThrow("purchaseDateGregorian")))
                obj.put("purchaseDateEthiopian", cursor.getString(cursor.getColumnIndexOrThrow("purchaseDateEthiopian")))
                obj.put("quantity", cursor.getInt(cursor.getColumnIndexOrThrow("quantity")))
                obj.put("purchasePrice", cursor.getDouble(cursor.getColumnIndexOrThrow("purchasePrice")))
                obj.put("sellingPrice", cursor.getDouble(cursor.getColumnIndexOrThrow("sellingPrice")))
                obj.put("customizationDetails", cursor.getString(cursor.getColumnIndexOrThrow("customizationDetails")))
                obj.put("recipient", cursor.getString(cursor.getColumnIndexOrThrow("recipient")))
                obj.put("createdAt", cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
                previousGifts.put(obj)
            }
        }
        root.put("previous_gifts", previousGifts)

        // Expenses
        val expenses = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM expenses").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("title", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                obj.put("amount", cursor.getDouble(cursor.getColumnIndexOrThrow("amount")))
                obj.put("category", cursor.getString(cursor.getColumnIndexOrThrow("category")))
                obj.put("timestamp", cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")))
                obj.put("notes", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
                expenses.put(obj)
            }
        }
        root.put("expenses", expenses)

        // App Settings
        val settings = JSONArray()
        db.openHelper.readableDatabase.query("SELECT * FROM app_settings").use { cursor ->
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("key", cursor.getString(cursor.getColumnIndexOrThrow("key")))
                obj.put("value", cursor.getString(cursor.getColumnIndexOrThrow("value")))
                settings.put(obj)
            }
        }
        root.put("app_settings", settings)

        root.toString(2)
    }

    suspend fun parseBackupPreview(jsonContent: String): BackupPreview = withContext(Dispatchers.Default) {
        val root = JSONObject(jsonContent)
        val schemaVersion = root.optInt("schema_version", 1)
        if (schemaVersion <= 0 || schemaVersion > 3) {
            throw IllegalArgumentException("Unsupported backup format or schema version: $schemaVersion")
        }
        val exportTimestamp = root.optLong("export_timestamp", 0L)
        val productsCount = root.optJSONArray("products")?.length() ?: 0
        val salesCount = root.optJSONArray("sales")?.length() ?: 0
        val customersCount = root.optJSONArray("customers")?.length() ?: 0
        val celebrationsCount = root.optJSONArray("celebrations")?.length() ?: 0
        val movementCount = root.optJSONArray("inventory_movements")?.length() ?: 0
        val expenseCount = root.optJSONArray("expenses")?.length() ?: 0
        val settingCount = root.optJSONArray("app_settings")?.length() ?: 0

        BackupPreview(
            schemaVersion = schemaVersion,
            exportTimestamp = exportTimestamp,
            productCount = productsCount,
            saleCount = salesCount,
            customerCount = customersCount,
            celebrationCount = celebrationsCount,
            movementCount = movementCount,
            expenseCount = expenseCount,
            settingCount = settingCount
        )
    }

    suspend fun restoreBackupJson(jsonContent: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonContent)
        val schemaVersion = root.optInt("schema_version", 0)
        if (schemaVersion <= 0 || schemaVersion > 3) {
            throw IllegalArgumentException("Unsupported or invalid backup file schema version: $schemaVersion")
        }

        // Validate that if any entity array property is present, it is actually a valid JSON Array
        val arrayKeys = listOf("products", "sales", "customers", "celebrations", "categories", "inventory_movements", "previous_gifts", "expenses", "app_settings")
        for (key in arrayKeys) {
            if (root.has(key) && root.optJSONArray(key) == null) {
                throw IllegalArgumentException("Corrupted backup format: field '$key' must be a valid JSON array")
            }
        }

        val pArray = root.optJSONArray("products") ?: JSONArray()
        val sArray = root.optJSONArray("sales") ?: JSONArray()
        val cArray = root.optJSONArray("customers") ?: JSONArray()
        val celArray = root.optJSONArray("celebrations") ?: JSONArray()
        val catArray = root.optJSONArray("categories") ?: JSONArray()
        val movArray = root.optJSONArray("inventory_movements") ?: JSONArray()
        val giftArray = root.optJSONArray("previous_gifts") ?: JSONArray()
        val expArray = root.optJSONArray("expenses") ?: JSONArray()
        val setArray = root.optJSONArray("app_settings") ?: JSONArray()

        db.withTransaction {
            // Restore products
            val products = mutableListOf<ProductEntity>()
            for (i in 0 until pArray.length()) {
                val o = pArray.getJSONObject(i)
                products.add(
                    ProductEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        category = o.getString("category"),
                        purchasePrice = o.getDouble("purchasePrice"),
                        sellingPrice = o.getDouble("sellingPrice"),
                        stockQuantity = o.getInt("stockQuantity"),
                        lowStockThreshold = o.optInt("lowStockThreshold", 3),
                        description = o.optString("description", ""),
                        isArchived = o.optBoolean("isArchived", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Restore sales
            val sales = mutableListOf<SaleEntity>()
            for (i in 0 until sArray.length()) {
                val o = sArray.getJSONObject(i)
                sales.add(
                    SaleEntity(
                        id = o.getLong("id"),
                        productId = o.getLong("productId"),
                        productNameSnapshot = o.getString("productNameSnapshot"),
                        quantity = o.getInt("quantity"),
                        purchasePriceSnapshot = o.getDouble("purchasePriceSnapshot"),
                        sellingPriceSnapshot = o.getDouble("sellingPriceSnapshot"),
                        totalRevenue = o.getDouble("totalRevenue"),
                        totalCost = o.getDouble("totalCost"),
                        grossProfit = o.getDouble("grossProfit"),
                        saleTimestamp = o.getLong("saleTimestamp"),
                        ethiopianDateStr = o.getString("ethiopianDateStr"),
                        customerId = if (o.has("customerId") && !o.isNull("customerId")) o.getLong("customerId") else null,
                        customerNameSnapshot = if (o.has("customerNameSnapshot") && !o.isNull("customerNameSnapshot")) o.getString("customerNameSnapshot") else null,
                        notes = o.optString("notes", ""),
                        isVoided = o.optBoolean("isVoided", false),
                        voidReason = o.optString("voidReason", ""),
                        voidTimestamp = if (o.has("voidTimestamp") && !o.isNull("voidTimestamp")) o.getLong("voidTimestamp") else null
                    )
                )
            }

            // Restore customers
            val customers = mutableListOf<CustomerEntity>()
            for (i in 0 until cArray.length()) {
                val o = cArray.getJSONObject(i)
                val photoUriStr = o.optString("photoUri", "")
                val b64 = o.optString("photoBase64", "")
                val resolvedPhotoUri = if (b64.isNotBlank()) {
                    try {
                        val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                        val targetDir = if (context != null) {
                            java.io.File(context.filesDir, "customer_photos").apply { mkdirs() }
                        } else {
                            val tmpDir = java.io.File(System.getProperty("java.io.tmpdir") ?: "/tmp", "customer_photos")
                            tmpDir.apply { mkdirs() }
                        }
                        val restoredFile = java.io.File(targetDir, "cust_restored_${o.getLong("id")}.jpg")
                        restoredFile.writeBytes(bytes)
                        restoredFile.absolutePath
                    } catch (e: Exception) {
                        photoUriStr
                    }
                } else {
                    photoUriStr
                }

                customers.add(
                    CustomerEntity(
                        id = o.getLong("id"),
                        fullName = o.getString("fullName"),
                        phoneNumber = o.getString("phoneNumber"),
                        alternativePhone = o.optString("alternativePhone", ""),
                        photoUri = resolvedPhotoUri,
                        gender = o.optString("gender", ""),
                        address = o.optString("address", ""),
                        city = o.optString("city", "Addis Ababa"),
                        notes = o.optString("notes", ""),
                        giftPreferences = o.optString("giftPreferences", ""),
                        preferredLanguage = o.optString("preferredLanguage", "English"),
                        preferredContactMethod = o.optString("preferredContactMethod", "WhatsApp"),
                        marketingConsent = o.optBoolean("marketingConsent", true),
                        isArchived = o.optBoolean("isArchived", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Restore celebrations
            val celebrations = mutableListOf<CelebrationEntity>()
            for (i in 0 until celArray.length()) {
                val o = celArray.getJSONObject(i)
                celebrations.add(
                    CelebrationEntity(
                        id = o.getLong("id"),
                        customerId = o.getLong("customerId"),
                        celebrationName = o.getString("celebrationName"),
                        celebrationCategory = o.optString("celebrationCategory", "Birthday"),
                        personCelebrated = o.getString("personCelebrated"),
                        relationship = o.optString("relationship", "Mother"),
                        ethMonth = o.getInt("ethMonth"),
                        ethDay = o.getInt("ethDay"),
                        recurrenceCycle = o.optString("recurrenceCycle", "YEARLY"),
                        reminderDaysBefore = o.optInt("reminderDaysBefore", 7),
                        notes = o.optString("notes", ""),
                        isActive = o.optBoolean("isActive", true),
                        lastNotifiedTimestamp = if (o.has("lastNotifiedTimestamp") && !o.isNull("lastNotifiedTimestamp")) o.getLong("lastNotifiedTimestamp") else null
                    )
                )
            }

            // Restore categories
            val categories = mutableListOf<CategoryEntity>()
            for (i in 0 until catArray.length()) {
                val o = catArray.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        iconName = o.optString("iconName", "gift")
                    )
                )
            }

            // Restore inventory movements
            val movements = mutableListOf<InventoryMovementEntity>()
            for (i in 0 until movArray.length()) {
                val o = movArray.getJSONObject(i)
                movements.add(
                    InventoryMovementEntity(
                        id = o.getLong("id"),
                        productId = o.getLong("productId"),
                        productNameSnapshot = o.getString("productNameSnapshot"),
                        movementType = o.getString("movementType"),
                        quantityDelta = o.getInt("quantityDelta"),
                        previousQuantity = o.getInt("previousQuantity"),
                        newQuantity = o.getInt("newQuantity"),
                        reason = o.optString("reason", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        ethiopianDateStr = o.optString("ethiopianDateStr", "")
                    )
                )
            }

            // Restore previous gifts
            val previousGifts = mutableListOf<com.example.data.local.entity.PreviousGiftEntity>()
            for (i in 0 until giftArray.length()) {
                val o = giftArray.getJSONObject(i)
                previousGifts.add(
                    com.example.data.local.entity.PreviousGiftEntity(
                        id = o.getLong("id"),
                        customerId = o.getLong("customerId"),
                        giftItemName = o.getString("giftItemName"),
                        purchaseDateGregorian = o.optString("purchaseDateGregorian", ""),
                        purchaseDateEthiopian = o.optString("purchaseDateEthiopian", ""),
                        quantity = o.optInt("quantity", 1),
                        purchasePrice = o.optDouble("purchasePrice", 0.0),
                        sellingPrice = o.optDouble("sellingPrice", 0.0),
                        customizationDetails = o.optString("customizationDetails", ""),
                        recipient = o.optString("recipient", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Restore expenses
            val expenses = mutableListOf<com.example.data.local.entity.ExpenseEntity>()
            for (i in 0 until expArray.length()) {
                val o = expArray.getJSONObject(i)
                expenses.add(
                    com.example.data.local.entity.ExpenseEntity(
                        id = o.getLong("id"),
                        title = o.getString("title"),
                        amount = o.getDouble("amount"),
                        category = o.optString("category", "General"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        notes = o.optString("notes", "")
                    )
                )
            }

            // Restore settings
            val settings = mutableListOf<com.example.data.local.entity.AppSettingsEntity>()
            for (i in 0 until setArray.length()) {
                val o = setArray.getJSONObject(i)
                settings.add(
                    com.example.data.local.entity.AppSettingsEntity(
                        key = o.getString("key"),
                        value = o.getString("value")
                    )
                )
            }

            // Clear old data and insert restored records safely
            db.productDao().clearAll()
            db.saleDao().clearAll()
            db.customerDao().clearAll()
            db.celebrationDao().clearAll()
            db.inventoryMovementDao().clearAll()
            db.previousGiftDao().clearAll()
            db.expenseDao().clearAll()
            if (categories.isNotEmpty()) {
                db.categoryDao().clearAll()
                db.categoryDao().insertAll(categories)
            }

            if (products.isNotEmpty()) db.productDao().insertAll(products)
            if (sales.isNotEmpty()) db.saleDao().insertAll(sales)
            if (customers.isNotEmpty()) db.customerDao().insertAll(customers)
            if (celebrations.isNotEmpty()) db.celebrationDao().insertAll(celebrations)
            if (movements.isNotEmpty()) db.inventoryMovementDao().insertAll(movements)
            if (previousGifts.isNotEmpty()) db.previousGiftDao().insertAll(previousGifts)
            if (expenses.isNotEmpty()) db.expenseDao().insertAll(expenses)
            if (settings.isNotEmpty()) {
                db.appSettingsDao().clearAll()
                db.appSettingsDao().insertAll(settings)
            }
        }
    }
}

data class BackupPreview(
    val schemaVersion: Int,
    val exportTimestamp: Long,
    val productCount: Int,
    val saleCount: Int,
    val customerCount: Int,
    val celebrationCount: Int,
    val movementCount: Int = 0,
    val expenseCount: Int = 0,
    val settingCount: Int = 0
)
