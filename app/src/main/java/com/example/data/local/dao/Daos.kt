package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CelebrationEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.PreviousGiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

data class InventorySummary(
    val totalProductsCount: Int,
    val totalStockQuantity: Int,
    val totalCostValue: Double,
    val lowStockCount: Int
)

data class SalesSummary(
    val totalRevenue: Double,
    val totalGrossProfit: Double,
    val salesCount: Int
)

data class ProductSalesPerformanceRow(
    val productName: String,
    val totalQuantitySold: Int,
    val totalRevenue: Double,
    val totalProfit: Double
)

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun getActiveProductsPaged(limit: Int, offset: Int): List<ProductEntity>

    @Query("""
        SELECT * FROM products 
        WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY name ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchProductsPaged(query: String, limit: Int, offset: Int): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products WHERE isArchived = 0")
    fun getActiveProductsCount(): Flow<Int>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE isArchived = 0 AND stockQuantity <= lowStockThreshold ORDER BY stockQuantity ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT 
            COUNT(*) AS totalProductsCount,
            COALESCE(SUM(stockQuantity), 0) AS totalStockQuantity,
            COALESCE(SUM(stockQuantity * purchasePrice), 0.0) AS totalCostValue,
            COALESCE(SUM(CASE WHEN stockQuantity <= lowStockThreshold THEN 1 ELSE 0 END), 0) AS lowStockCount
        FROM products
        WHERE isArchived = 0
    """)
    fun getInventorySummary(): Flow<InventorySummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET isArchived = 1 WHERE id = :id")
    suspend fun archiveProduct(id: Long)

    @Query("UPDATE products SET stockQuantity = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY saleTimestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY saleTimestamp DESC LIMIT :limit")
    fun getRecentSales(limit: Int = 100): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY saleTimestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesPaged(limit: Int, offset: Int): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE saleTimestamp >= :start AND saleTimestamp <= :end ORDER BY saleTimestamp DESC")
    fun getSalesBetween(start: Long, end: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE saleTimestamp >= :start AND saleTimestamp <= :end ORDER BY saleTimestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getSalesBetweenPaged(start: Long, end: Long, limit: Int, offset: Int): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY saleTimestamp DESC")
    fun getSalesForCustomer(customerId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY saleTimestamp DESC LIMIT :limit")
    fun getRecentSalesForCustomer(customerId: Long, limit: Int = 10): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Query("""
        SELECT 
            COALESCE(SUM(totalRevenue), 0.0) AS totalRevenue,
            COALESCE(SUM(grossProfit), 0.0) AS totalGrossProfit,
            COUNT(*) AS salesCount
        FROM sales
        WHERE isVoided = 0
    """)
    fun getLifetimeSalesSummary(): Flow<SalesSummary>

    @Query("""
        SELECT 
            COALESCE(SUM(totalRevenue), 0.0) AS totalRevenue,
            COALESCE(SUM(grossProfit), 0.0) AS totalGrossProfit,
            COUNT(*) AS salesCount
        FROM sales
        WHERE isVoided = 0 AND saleTimestamp >= :start AND saleTimestamp <= :end
    """)
    fun getSalesSummaryBetween(start: Long, end: Long): Flow<SalesSummary>

    @Query("""
        SELECT 
            productNameSnapshot AS productName,
            SUM(quantity) AS totalQuantitySold,
            COALESCE(SUM(totalRevenue), 0.0) AS totalRevenue,
            COALESCE(SUM(grossProfit), 0.0) AS totalProfit
        FROM sales
        WHERE isVoided = 0
        GROUP BY productNameSnapshot
        ORDER BY totalQuantitySold DESC
        LIMIT :limit
    """)
    fun getTopSellingProducts(limit: Int = 10): Flow<List<ProductSalesPerformanceRow>>

    @Query("SELECT COUNT(*) FROM sales WHERE isVoided = 0")
    fun getValidSalesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sales WHERE isVoided = 0 AND saleTimestamp >= :start AND saleTimestamp <= :end")
    suspend fun getSalesCountBetween(start: Long, end: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<SaleEntity>)

    @Update
    suspend fun update(sale: SaleEntity)

    @Query("UPDATE sales SET isVoided = 1, voidReason = :reason, voidTimestamp = :timestamp WHERE id = :id")
    suspend fun voidSale(id: Long, reason: String, timestamp: Long)

    @Query("DELETE FROM sales")
    suspend fun clearAll()
}

@Dao
interface InventoryMovementDao {
    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMovements(limit: Int = 100): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMovementsPaged(limit: Int, offset: Int): List<InventoryMovementEntity>

    @Query("SELECT * FROM inventory_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<InventoryMovementEntity>>

    @Query("SELECT COUNT(*) FROM inventory_movements")
    fun getMovementsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: InventoryMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<InventoryMovementEntity>)

    @Query("DELETE FROM inventory_movements")
    suspend fun clearAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY fullName ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY fullName ASC")
    fun getActiveCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY fullName ASC LIMIT :limit OFFSET :offset")
    suspend fun getActiveCustomersPaged(limit: Int, offset: Int): List<CustomerEntity>

    @Query("""
        SELECT * FROM customers 
        WHERE isArchived = 0 AND (fullName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%')
        ORDER BY fullName ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchCustomersPaged(query: String, limit: Int, offset: Int): List<CustomerEntity>

    @Query("SELECT COUNT(*) FROM customers WHERE isArchived = 0")
    fun getActiveCustomersCount(): Flow<Int>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCustomer(id: Long)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun clearAll()
}

@Dao
interface CelebrationDao {
    @Query("SELECT * FROM celebrations ORDER BY ethMonth ASC, ethDay ASC")
    fun getAllCelebrations(): Flow<List<CelebrationEntity>>

    @Query("SELECT * FROM celebrations WHERE isActive = 1 ORDER BY ethMonth ASC, ethDay ASC")
    fun getActiveCelebrations(): Flow<List<CelebrationEntity>>

    @Query("""
        SELECT * FROM celebrations 
        WHERE isActive = 1 AND ethMonth IN (:month1, :month2)
        ORDER BY ethMonth ASC, ethDay ASC
    """)
    suspend fun getActiveCelebrationsForMonths(month1: Int, month2: Int): List<CelebrationEntity>

    @Query("SELECT * FROM celebrations WHERE isActive = 1 ORDER BY ethMonth ASC, ethDay ASC LIMIT :limit OFFSET :offset")
    suspend fun getActiveCelebrationsPaged(limit: Int, offset: Int): List<CelebrationEntity>

    @Query("SELECT COUNT(*) FROM celebrations WHERE isActive = 1")
    fun getActiveCelebrationsCount(): Flow<Int>

    @Query("SELECT * FROM celebrations WHERE customerId = :customerId")
    fun getCelebrationsForCustomer(customerId: Long): Flow<List<CelebrationEntity>>

    @Query("SELECT * FROM celebrations WHERE id = :id LIMIT 1")
    suspend fun getCelebrationById(id: Long): CelebrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(celebration: CelebrationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(celebrations: List<CelebrationEntity>)

    @Update
    suspend fun update(celebration: CelebrationEntity)

    @Delete
    suspend fun delete(celebration: CelebrationEntity)

    @Query("UPDATE celebrations SET lastNotifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastNotified(id: Long, timestamp: Long)

    @Query("DELETE FROM celebrations")
    suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getExpensesPaged(limit: Int, offset: Int): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses")
    fun getTotalExpenses(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE timestamp >= :start AND timestamp <= :end")
    fun getExpensesBetween(start: Long, end: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingsEntity)

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettings(): List<AppSettingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<AppSettingsEntity>)

    @Query("DELETE FROM app_settings")
    suspend fun clearAll()
}

@Dao
interface PreviousGiftDao {
    @Query("SELECT * FROM previous_gifts WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getPreviousGiftsForCustomer(customerId: Long): Flow<List<PreviousGiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gift: PreviousGiftEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gifts: List<PreviousGiftEntity>)

    @Delete
    suspend fun delete(gift: PreviousGiftEntity)

    @Query("DELETE FROM previous_gifts")
    suspend fun clearAll()
}
