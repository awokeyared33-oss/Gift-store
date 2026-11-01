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

class MakiRepository(private val db: MakiDatabase) {

    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val activeProducts: Flow<List<ProductEntity>> = db.productDao().getActiveProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    val allSales: Flow<List<SaleEntity>> = db.saleDao().getAllSales()
    val allMovements: Flow<List<InventoryMovementEntity>> = db.inventoryMovementDao().getAllMovements()
    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
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
        root.put("schema_version", 1)
        root.put("app_name", "MAKI BRANDS")
        root.put("app_version", "1.0.0")
        root.put("export_timestamp", System.currentTimeMillis())

        // Products
        val products = JSONArray()
        // Query synchronous lists for backup
        db.run {
            // Read all entities
        }
        val pList = db.openHelper.readableDatabase.query("SELECT * FROM products").use { cursor ->
            val list = mutableListOf<JSONObject>()
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
                list.add(obj)
            }
            list
        }
        pList.forEach { products.put(it) }
        root.put("products", products)

        // Sales
        val sales = JSONArray()
        val sList = db.openHelper.readableDatabase.query("SELECT * FROM sales").use { cursor ->
            val list = mutableListOf<JSONObject>()
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
                list.add(obj)
            }
            list
        }
        sList.forEach { sales.put(it) }
        root.put("sales", sales)

        // Customers
        val customers = JSONArray()
        val cList = db.openHelper.readableDatabase.query("SELECT * FROM customers").use { cursor ->
            val list = mutableListOf<JSONObject>()
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("fullName", cursor.getString(cursor.getColumnIndexOrThrow("fullName")))
                obj.put("phoneNumber", cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")))
                obj.put("notes", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
                obj.put("createdAt", cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")))
                list.add(obj)
            }
            list
        }
        cList.forEach { customers.put(it) }
        root.put("customers", customers)

        // Celebrations
        val celebrations = JSONArray()
        val celList = db.openHelper.readableDatabase.query("SELECT * FROM celebrations").use { cursor ->
            val list = mutableListOf<JSONObject>()
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("customerId", cursor.getLong(cursor.getColumnIndexOrThrow("customerId")))
                obj.put("celebrationName", cursor.getString(cursor.getColumnIndexOrThrow("celebrationName")))
                obj.put("personCelebrated", cursor.getString(cursor.getColumnIndexOrThrow("personCelebrated")))
                obj.put("ethMonth", cursor.getInt(cursor.getColumnIndexOrThrow("ethMonth")))
                obj.put("ethDay", cursor.getInt(cursor.getColumnIndexOrThrow("ethDay")))
                obj.put("recurrenceCycle", cursor.getString(cursor.getColumnIndexOrThrow("recurrenceCycle")))
                obj.put("reminderDaysBefore", cursor.getInt(cursor.getColumnIndexOrThrow("reminderDaysBefore")))
                obj.put("notes", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
                obj.put("isActive", cursor.getInt(cursor.getColumnIndexOrThrow("isActive")) == 1)
                val notIdx = cursor.getColumnIndexOrThrow("lastNotifiedTimestamp")
                if (!cursor.isNull(notIdx)) obj.put("lastNotifiedTimestamp", cursor.getLong(notIdx))
                list.add(obj)
            }
            list
        }
        celList.forEach { celebrations.put(it) }
        root.put("celebrations", celebrations)

        // Categories
        val categories = JSONArray()
        val catList = db.openHelper.readableDatabase.query("SELECT * FROM categories").use { cursor ->
            val list = mutableListOf<JSONObject>()
            while (cursor.moveToNext()) {
                val obj = JSONObject()
                obj.put("id", cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                obj.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                obj.put("iconName", cursor.getString(cursor.getColumnIndexOrThrow("iconName")))
                list.add(obj)
            }
            list
        }
        catList.forEach { categories.put(it) }
        root.put("categories", categories)

        root.toString(2)
    }

    suspend fun parseBackupPreview(jsonContent: String): BackupPreview = withContext(Dispatchers.Default) {
        val root = JSONObject(jsonContent)
        val schemaVersion = root.optInt("schema_version", 1)
        val exportTimestamp = root.optLong("export_timestamp", 0L)
        val productsCount = root.optJSONArray("products")?.length() ?: 0
        val salesCount = root.optJSONArray("sales")?.length() ?: 0
        val customersCount = root.optJSONArray("customers")?.length() ?: 0
        val celebrationsCount = root.optJSONArray("celebrations")?.length() ?: 0

        BackupPreview(
            schemaVersion = schemaVersion,
            exportTimestamp = exportTimestamp,
            productCount = productsCount,
            saleCount = salesCount,
            customerCount = customersCount,
            celebrationCount = celebrationsCount
        )
    }

    suspend fun restoreBackupJson(jsonContent: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonContent)
        val pArray = root.optJSONArray("products") ?: JSONArray()
        val sArray = root.optJSONArray("sales") ?: JSONArray()
        val cArray = root.optJSONArray("customers") ?: JSONArray()
        val celArray = root.optJSONArray("celebrations") ?: JSONArray()
        val catArray = root.optJSONArray("categories") ?: JSONArray()

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
                customers.add(
                    CustomerEntity(
                        id = o.getLong("id"),
                        fullName = o.getString("fullName"),
                        phoneNumber = o.getString("phoneNumber"),
                        notes = o.optString("notes", ""),
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
                        personCelebrated = o.getString("personCelebrated"),
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

            // Clear old data and insert restored records safely
            db.productDao().clearAll()
            db.saleDao().clearAll()
            db.customerDao().clearAll()
            db.celebrationDao().clearAll()

            if (products.isNotEmpty()) db.productDao().insertAll(products)
            if (sales.isNotEmpty()) db.saleDao().insertAll(sales)
            if (customers.isNotEmpty()) db.customerDao().insertAll(customers)
            if (celebrations.isNotEmpty()) db.celebrationDao().insertAll(celebrations)
        }
    }
}

data class BackupPreview(
    val schemaVersion: Int,
    val exportTimestamp: Long,
    val productCount: Int,
    val saleCount: Int,
    val customerCount: Int,
    val celebrationCount: Int
)
