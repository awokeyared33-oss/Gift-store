package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MakiDatabase
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CelebrationEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.PreviousGiftEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.MakiRepository
import com.example.domain.calculator.FinancialCalculator
import com.example.domain.calendar.EthiopianCalendarConverter
import com.example.domain.calendar.EthiopianDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MakiBusinessLogicTest {

    private lateinit var db: MakiDatabase
    private lateinit var repository: MakiRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, MakiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MakiRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testExactSalesCalculationFromSpecification() {
        // Buy price: 150 ETB, Sell price: 250 ETB, Quantity: 2
        val qty = 2
        val buyPrice = BigDecimal("150.00")
        val sellPrice = BigDecimal("250.00")

        val financials = FinancialCalculator.calculateSaleFinancials(
            quantity = qty,
            purchasePrice = buyPrice,
            sellingPrice = sellPrice
        )

        // Expected revenue: 500 ETB
        assertEquals(BigDecimal("500.00"), financials.revenue)
        // Expected cost: 300 ETB
        assertEquals(BigDecimal("300.00"), financials.cost)
        // Expected gross profit: 200 ETB
        assertEquals(BigDecimal("200.00"), financials.grossProfit)
        // Profit margin: (200 / 500) * 100 = 40.0%
        assertEquals(40.0, financials.profitMarginPercent, 0.01)
        assertFalse(financials.isSellingAtLoss)
    }

    @Test
    fun testSellingAtLossWarning() {
        val qty = 1
        val buyPrice = BigDecimal("300.00")
        val sellPrice = BigDecimal("250.00")

        val financials = FinancialCalculator.calculateSaleFinancials(
            quantity = qty,
            purchasePrice = buyPrice,
            sellingPrice = sellPrice
        )

        assertEquals(BigDecimal("-50.00"), financials.grossProfit)
        assertTrue(financials.isSellingAtLoss)
    }

    @Test
    fun testInventoryCostValueCalculation() {
        val qty = 25
        val buyPrice = BigDecimal("120.50")
        val totalValue = FinancialCalculator.calculateInventoryValue(qty, buyPrice)

        assertEquals(BigDecimal("3012.50"), totalValue)
    }

    @Test
    fun testRecordSaleDecrementsStockAndSavesSnapshots() = runBlocking {
        // 1. Create a product with purchasePrice 150, sellingPrice 250, stock 10
        val prod = ProductEntity(
            name = "Special Gift Hamper",
            category = "Gift Box",
            purchasePrice = 150.0,
            sellingPrice = 250.0,
            stockQuantity = 10,
            lowStockThreshold = 3
        )
        val prodId = repository.saveProduct(prod)

        // 2. Create customer
        val cust = CustomerEntity(
            fullName = "Dawit Tadesse",
            phoneNumber = "0911223344",
            city = "Addis Ababa"
        )
        val custId = repository.saveCustomer(cust)

        // 3. Record Sale of 2 units
        val sale = repository.recordSale(
            productId = prodId,
            quantity = 2,
            customerId = custId,
            notes = "Anniversary gift"
        )

        // 4. Verify financial snapshots
        assertEquals(500.0, sale.totalRevenue, 0.001)
        assertEquals(300.0, sale.totalCost, 0.001)
        assertEquals(200.0, sale.grossProfit, 0.001)
        assertEquals(150.0, sale.purchasePriceSnapshot, 0.001)
        assertEquals(250.0, sale.sellingPriceSnapshot, 0.001)

        // 5. Verify stock decremented to 8
        val updatedProd = db.productDao().getProductById(prodId)
        assertNotNull(updatedProd)
        assertEquals(8, updatedProd!!.stockQuantity)

        // 6. Verify inventory movement created
        val movements = repository.allMovements.first()
        val saleMovement = movements.firstOrNull { it.productId == prodId }
        assertNotNull(saleMovement)
        assertEquals("SALE_DEDUCTION", saleMovement!!.movementType)
        assertEquals(-2, saleMovement.quantityDelta)
        assertEquals(10, saleMovement.previousQuantity)
        assertEquals(8, saleMovement.newQuantity)

        // 7. Verify previous gift entity recorded for customer CRM
        val gifts = db.previousGiftDao().getPreviousGiftsForCustomer(custId).first()
        assertEquals(1, gifts.size)
        assertEquals("Special Gift Hamper", gifts[0].giftItemName)
    }

    @Test
    fun testRecordSaleRejectsInsufficientStock() = runBlocking {
        val prod = ProductEntity(
            name = "Limited Perfume",
            category = "Perfume",
            purchasePrice = 300.0,
            sellingPrice = 450.0,
            stockQuantity = 1,
            lowStockThreshold = 1
        )
        val prodId = repository.saveProduct(prod)

        try {
            repository.recordSale(
                productId = prodId,
                quantity = 5, // Exceeds available stock (1)
                customerId = null,
                notes = ""
            )
            fail("Expected IllegalStateException for insufficient stock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Insufficient stock"))
        }

        // Verify stock is still 1
        val checkProd = db.productDao().getProductById(prodId)
        assertEquals(1, checkProd!!.stockQuantity)
    }

    @Test
    fun testHistoricalPricesPreservedWhenProductPricesChange() = runBlocking {
        // Create product
        val prodId = repository.saveProduct(
            ProductEntity(
                name = "Box of Luxury Truffles",
                category = "Chocolate",
                purchasePrice = 150.0,
                sellingPrice = 250.0,
                stockQuantity = 20,
                lowStockThreshold = 5
            )
        )

        // Record sale at 250 ETB
        val sale = repository.recordSale(
            productId = prodId,
            quantity = 2,
            customerId = null
        )
        assertEquals(500.0, sale.totalRevenue, 0.001)
        assertEquals(200.0, sale.grossProfit, 0.001)

        // Now supplier price increases and selling price increases to 400 ETB
        val currentProd = db.productDao().getProductById(prodId)!!
        repository.saveProduct(
            currentProd.copy(
                purchasePrice = 200.0,
                sellingPrice = 400.0
            )
        )

        // Verify past sale record is unchanged
        val storedSale = db.saleDao().getSaleById(sale.id)
        assertNotNull(storedSale)
        assertEquals(150.0, storedSale!!.purchasePriceSnapshot, 0.001)
        assertEquals(250.0, storedSale.sellingPriceSnapshot, 0.001)
        assertEquals(500.0, storedSale.totalRevenue, 0.001)
        assertEquals(200.0, storedSale.grossProfit, 0.001)
    }

    @Test
    fun testVoidSaleRestoresStockAndAudits() = runBlocking {
        val prodId = repository.saveProduct(
            ProductEntity(
                name = "Silver Photo Frame",
                category = "Gift Box",
                purchasePrice = 100.0,
                sellingPrice = 180.0,
                stockQuantity = 5,
                lowStockThreshold = 2
            )
        )

        val sale = repository.recordSale(
            productId = prodId,
            quantity = 3,
            customerId = null
        )
        assertEquals(2, db.productDao().getProductById(prodId)!!.stockQuantity)

        // Void the sale
        repository.voidSale(sale.id, "Customer returned unopened item")

        val voidedSale = db.saleDao().getSaleById(sale.id)!!
        assertTrue(voidedSale.isVoided)
        assertEquals("Customer returned unopened item", voidedSale.voidReason)

        // Verify stock restored to 5
        val restoredProd = db.productDao().getProductById(prodId)!!
        assertEquals(5, restoredProd.stockQuantity)

        // Verify movement audit trail
        val movements = repository.allMovements.first()
        val voidMovement = movements.firstOrNull { it.movementType == "SALE_VOID_RESTORE" }
        assertNotNull(voidMovement)
        assertEquals(3, voidMovement!!.quantityDelta)
    }

    @Test
    fun testDamageLossAdjustment() = runBlocking {
        val prodId = repository.saveProduct(
            ProductEntity(
                name = "Glass Vase",
                category = "Decor",
                purchasePrice = 80.0,
                sellingPrice = 150.0,
                stockQuantity = 10,
                lowStockThreshold = 2
            )
        )

        // Record damage of 2 units
        repository.adjustInventory(
            productId = prodId,
            quantityDelta = -2,
            isDamage = true,
            reason = "Accidentally broken in showroom display"
        )

        val updatedProd = db.productDao().getProductById(prodId)!!
        assertEquals(8, updatedProd.stockQuantity)

        val movements = repository.allMovements.first()
        val damageMovement = movements.firstOrNull { it.movementType == "DAMAGE_LOSS" }
        assertNotNull(damageMovement)
        assertEquals(-2, damageMovement!!.quantityDelta)
    }

    @Test
    fun testCustomerArchivingPreservesRecords() = runBlocking {
        val custId = repository.saveCustomer(
            CustomerEntity(
                fullName = "Selamawit Kebede",
                phoneNumber = "0922334455",
                city = "Addis Ababa"
            )
        )

        // Verify customer in active list
        val activeBefore = repository.activeCustomers.first()
        assertTrue(activeBefore.any { it.id == custId })

        // Archive customer
        repository.archiveCustomer(custId)

        // Active list should no longer include customer
        val activeAfter = repository.activeCustomers.first()
        assertFalse(activeAfter.any { it.id == custId })

        // All customers list still preserves customer for historical records
        val allAfter = repository.allCustomers.first()
        assertTrue(allAfter.any { it.id == custId && it.isArchived })
    }

    @Test
    fun testEthiopianCalendarRecurrenceCycles() {
        val baseDate = LocalDate.of(2024, 9, 20)
        val currentEth = EthiopianCalendarConverter.toEthiopian(baseDate)

        // 1. YEARLY (Meskerem 5 has passed in current Ethiopian year as baseDate is Meskerem 10)
        val nextYearly = EthiopianCalendarConverter.getNextCelebrationDate(
            fromDate = baseDate,
            ethMonth = 1, // Meskerem
            ethDay = 5,   // Passed 5 days ago in year 2017
            recurrenceCycle = "YEARLY"
        )
        val nextYearlyEth = EthiopianCalendarConverter.toEthiopian(nextYearly)
        assertEquals(currentEth.year + 1, nextYearlyEth.year)
        assertEquals(1, nextYearlyEth.month)
        assertEquals(5, nextYearlyEth.day)

        // 2. MONTHLY (Meskerem 5 has passed, so next monthly occurrence is Tikimt 5)
        val nextMonthly = EthiopianCalendarConverter.getNextCelebrationDate(
            fromDate = baseDate,
            ethMonth = 1,
            ethDay = 5,
            recurrenceCycle = "MONTHLY"
        )
        val nextMonthlyEth = EthiopianCalendarConverter.toEthiopian(nextMonthly)
        assertEquals(2, nextMonthlyEth.month) // Next month: Tikimt
        assertEquals(5, nextMonthlyEth.day)
    }

    @Test
    fun testEthiopianCalendarNewYearConversion() {
        // Meskerem 1, 2017 Ethiopian Calendar corresponds to September 11, 2024 Gregorian
        val gregDate = LocalDate.of(2024, 9, 11)
        val ethDate = EthiopianCalendarConverter.toEthiopian(gregDate)

        assertEquals(2017, ethDate.year)
        assertEquals(1, ethDate.month) // Meskerem
        assertEquals(1, ethDate.day)

        // Roundtrip back to Gregorian
        val backToGreg = EthiopianCalendarConverter.toGregorian(ethDate)
        assertEquals(gregDate, backToGreg)
    }

    @Test
    fun testEthiopianCalendarPagumeMonth13AndLeapYear() {
        assertTrue(EthiopianDate.isEthiopianLeapYear(2015)) // 2015 % 4 == 3
        assertFalse(EthiopianDate.isEthiopianLeapYear(2016)) // 2016 % 4 == 0
        assertFalse(EthiopianDate.isEthiopianLeapYear(2017)) // 2017 % 4 == 1

        val pagumeDate = EthiopianDate(2016, 13, 5)
        assertEquals(13, pagumeDate.month)
        assertEquals(5, pagumeDate.day)

        val pagumeGreg = EthiopianCalendarConverter.toGregorian(pagumeDate)
        val ethConverted = EthiopianCalendarConverter.toEthiopian(pagumeGreg)
        assertEquals(pagumeDate.year, ethConverted.year)
        assertEquals(pagumeDate.month, ethConverted.month)
        assertEquals(pagumeDate.day, ethConverted.day)
    }

    @Test
    fun testBackupExportAndAtomicRestore() = runBlocking {
        // Seed database
        val prodId = repository.saveProduct(
            ProductEntity(
                name = "Backup Test Product",
                category = "Gift Box",
                purchasePrice = 100.0,
                sellingPrice = 180.0,
                stockQuantity = 15,
                lowStockThreshold = 2
            )
        )
        val custId = repository.saveCustomer(
            CustomerEntity(
                fullName = "Backup Test Customer",
                phoneNumber = "0988776655"
            )
        )
        repository.recordSale(
            productId = prodId,
            quantity = 2,
            customerId = custId
        )

        // Export backup
        val json = repository.exportBackupJson()
        assertTrue(json.isNotBlank())

        val preview = repository.parseBackupPreview(json)
        assertTrue(preview.productCount >= 1)
        assertTrue(preview.customerCount >= 1)
        assertTrue(preview.saleCount >= 1)

        // Modify local state
        repository.saveProduct(
            ProductEntity(name = "Temporary Product", category = "Temp", purchasePrice = 10.0, sellingPrice = 20.0, stockQuantity = 1)
        )

        // Restore backup
        repository.restoreBackupJson(json)

        // Verify restoration
        val restoredProducts = repository.allProducts.first()
        assertTrue(restoredProducts.any { it.name == "Backup Test Product" })
        assertFalse(restoredProducts.any { it.name == "Temporary Product" })
    }

    @Test
    fun testRestoreRejectsInvalidJson() = runBlocking {
        val corruptedJson = "{ invalid: json... }"
        try {
            repository.restoreBackupJson(corruptedJson)
            fail("Expected exception for corrupted json")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    fun testExactRealWorldSpecificationScenarioWithPriceChangeAndVoid() = runBlocking {
        // Create product:
        // Purchase price: 150 ETB, Selling price: 250 ETB, Initial stock: 25.
        val prodId = repository.saveProduct(
            ProductEntity(
                name = "Luxury Gift Basket",
                category = "Gift Box",
                purchasePrice = 150.0,
                sellingPrice = 250.0,
                stockQuantity = 25,
                lowStockThreshold = 5
            )
        )

        // Sell quantity: 2
        val sale = repository.recordSale(
            productId = prodId,
            quantity = 2
        )

        // Expected:
        // Revenue: 500 ETB, Cost: 300 ETB, Gross profit: 200 ETB, Remaining stock: 23
        assertEquals(500.0, sale.totalRevenue, 0.001)
        assertEquals(300.0, sale.totalCost, 0.001)
        assertEquals(200.0, sale.grossProfit, 0.001)

        val stockAfterSale = db.productDao().getProductById(prodId)?.stockQuantity
        assertEquals(23, stockAfterSale)

        // Close and reopen application simulation (query fresh instance from database)
        val freshSale = db.saleDao().getSaleById(sale.id)
        assertNotNull(freshSale)
        assertEquals(500.0, freshSale!!.totalRevenue, 0.001)
        assertEquals(300.0, freshSale.totalCost, 0.001)
        assertEquals(200.0, freshSale.grossProfit, 0.001)

        val freshProd = db.productDao().getProductById(prodId)
        assertNotNull(freshProd)
        assertEquals(23, freshProd!!.stockQuantity)

        // Change the product selling price to 400 ETB
        repository.saveProduct(freshProd.copy(sellingPrice = 400.0))

        // Verify that the previous sale STILL displays 250 ETB selling price, 500 ETB revenue, and 200 ETB gross profit
        val pastSaleAfterPriceChange = db.saleDao().getSaleById(sale.id)
        assertNotNull(pastSaleAfterPriceChange)
        assertEquals(250.0, pastSaleAfterPriceChange!!.sellingPriceSnapshot, 0.001)
        assertEquals(500.0, pastSaleAfterPriceChange.totalRevenue, 0.001)
        assertEquals(300.0, pastSaleAfterPriceChange.totalCost, 0.001)
        assertEquals(200.0, pastSaleAfterPriceChange.grossProfit, 0.001)

        // Test voiding and refunding
        repository.voidSale(sale.id, "Customer return")

        // Verify inventory is restored exactly once to 25
        val stockAfterVoid = db.productDao().getProductById(prodId)?.stockQuantity
        assertEquals(25, stockAfterVoid)

        val voidedSale = db.saleDao().getSaleById(sale.id)
        assertTrue(voidedSale?.isVoided == true)

        // Second void attempt should be rejected without altering stock
        try {
            repository.voidSale(sale.id, "Attempt duplicate void")
            fail("Expected exception when voiding an already voided sale")
        } catch (e: Exception) {
            // Expected
        }
        val stockAfterSecondVoidAttempt = db.productDao().getProductById(prodId)?.stockQuantity
        assertEquals(25, stockAfterSecondVoidAttempt)
    }

    @Test
    fun testFullDatabaseBackupCoverageAllNineEntitiesAndNoDuplicates() = runBlocking {
        // 1. Products
        val pId = repository.saveProduct(
            ProductEntity(name = "Perfume Set", category = "Perfume", purchasePrice = 200.0, sellingPrice = 350.0, stockQuantity = 10)
        )
        // 2. Categories
        db.categoryDao().insert(CategoryEntity(id = 99L, name = "Special Hampers", iconName = "cake"))
        // 3. Customers
        val cId = repository.saveCustomer(CustomerEntity(fullName = "Almaz Bekele", phoneNumber = "0922334455", photoUri = "file:///sample.jpg"))
        // 4. Celebrations
        db.celebrationDao().insert(
            CelebrationEntity(customerId = cId, celebrationName = "Anniversary", ethMonth = 1, ethDay = 15, recurrenceCycle = "YEARLY", personCelebrated = "Spouse")
        )
        // 5. Sales
        val s = repository.recordSale(productId = pId, quantity = 1, customerId = cId)
        // 6. Inventory Movements
        repository.adjustInventory(productId = pId, quantityDelta = 5, isDamage = false, reason = "Supplier Restock")
        // 7. Previous Gifts
        db.previousGiftDao().insert(
            PreviousGiftEntity(customerId = cId, giftItemName = "Handmade Mug", purchasePrice = 50.0, sellingPrice = 120.0, quantity = 1)
        )
        // 8. Expenses
        db.expenseDao().insert(
            ExpenseEntity(title = "Packaging Paper", amount = 150.0, category = "Supplies")
        )
        // 9. App Settings
        db.appSettingsDao().setSetting(
            AppSettingsEntity(key = "currency", value = "ETB")
        )

        // Export full JSON backup
        val json = repository.exportBackupJson()
        assertTrue(json.contains("\"products\""))
        assertTrue(json.contains("\"categories\""))
        assertTrue(json.contains("\"sales\""))
        assertTrue(json.contains("\"inventory_movements\""))
        assertTrue(json.contains("\"customers\""))
        assertTrue(json.contains("\"celebrations\""))
        assertTrue(json.contains("\"previous_gifts\""))
        assertTrue(json.contains("\"expenses\""))
        assertTrue(json.contains("\"app_settings\""))

        val preview = repository.parseBackupPreview(json)
        assertEquals(1, preview.productCount)
        assertEquals(1, preview.customerCount)
        assertEquals(1, preview.celebrationCount)
        assertEquals(1, preview.saleCount)
        assertTrue(preview.movementCount >= 2)
        assertEquals(1, preview.expenseCount)

        // Restore into database
        repository.restoreBackupJson(json)

        // Verify all 9 entities exist post-restore
        assertEquals(1, db.productDao().getAllProducts().first().size)
        assertEquals(1, db.customerDao().getAllCustomers().first().size)
        assertEquals(1, db.celebrationDao().getAllCelebrations().first().size)
        assertEquals(1, db.saleDao().getAllSales().first().size)
        assertEquals(2, db.previousGiftDao().getPreviousGiftsForCustomer(cId).first().size)
        assertEquals(1, db.expenseDao().getAllExpenses().first().size)
        assertEquals("ETB", db.appSettingsDao().getSetting("currency")?.value)

        // Restore again -> verify NO duplicates created
        repository.restoreBackupJson(json)
        assertEquals(1, db.productDao().getAllProducts().first().size)
        assertEquals(1, db.customerDao().getAllCustomers().first().size)
        assertEquals(1, db.celebrationDao().getAllCelebrations().first().size)
        assertEquals(1, db.saleDao().getAllSales().first().size)
        assertEquals(2, db.previousGiftDao().getPreviousGiftsForCustomer(cId).first().size)
        assertEquals(1, db.expenseDao().getAllExpenses().first().size)

        // Corrupted JSON rollback verification
        try {
            repository.restoreBackupJson("{\"schema_version\": 2, \"products\": \"NOT_AN_ARRAY\"}")
            fail("Expected exception on invalid backup content")
        } catch (e: Exception) {
            // Expected - data must remain intact
        }
        assertEquals(1, db.productDao().getAllProducts().first().size)
    }

    @Test
    fun testEthiopianCalendarComprehensiveEdgeCasesAndPagume() {
        // 1. Leap year rule: Ethiopian leap year occurs when year % 4 == 3
        assertTrue(EthiopianDate.isEthiopianLeapYear(2015)) // 2015 % 4 = 3 (Leap Year)
        assertFalse(EthiopianDate.isEthiopianLeapYear(2016)) // 2016 % 4 = 0 (Standard Year)
        assertFalse(EthiopianDate.isEthiopianLeapYear(2017)) // 2017 % 4 = 1 (Standard Year)
        assertFalse(EthiopianDate.isEthiopianLeapYear(2018)) // 2018 % 4 = 2 (Standard Year)
        assertTrue(EthiopianDate.isEthiopianLeapYear(2019)) // 2019 % 4 = 3 (Leap Year)

        // 2. 6-day Pagume in Leap Year 2015 EC
        val pagume6_2015 = EthiopianDate(2015, 13, 6)
        val gDatePagume6 = EthiopianCalendarConverter.toGregorian(pagume6_2015)
        assertEquals(LocalDate.of(2023, 9, 11), gDatePagume6)
        val roundTripPagume6 = EthiopianCalendarConverter.toEthiopian(gDatePagume6)
        assertEquals(pagume6_2015, roundTripPagume6)

        // 3. New Year Meskerem 1, 2016 EC (follows leap year 2015 EC -> Sept 12)
        val meskerem1_2016 = EthiopianDate(2016, 1, 1)
        val gDateNewYear2016 = EthiopianCalendarConverter.toGregorian(meskerem1_2016)
        assertEquals(LocalDate.of(2023, 9, 12), gDateNewYear2016)
        assertEquals(meskerem1_2016, EthiopianCalendarConverter.toEthiopian(gDateNewYear2016))

        // 4. 5-day Pagume in Standard Year 2016 EC
        val pagume5_2016 = EthiopianDate(2016, 13, 5)
        val gDatePagume5_2016 = EthiopianCalendarConverter.toGregorian(pagume5_2016)
        assertEquals(LocalDate.of(2024, 9, 10), gDatePagume5_2016)
        assertEquals(pagume5_2016, EthiopianCalendarConverter.toEthiopian(gDatePagume5_2016))

        // 5. New Year Meskerem 1, 2017 EC (follows standard year 2016 EC -> Sept 11)
        val meskerem1_2017 = EthiopianDate(2017, 1, 1)
        val gDateNewYear2017 = EthiopianCalendarConverter.toGregorian(meskerem1_2017)
        assertEquals(LocalDate.of(2024, 9, 11), gDateNewYear2017)
        assertEquals(meskerem1_2017, EthiopianCalendarConverter.toEthiopian(gDateNewYear2017))

        // 6. 5-day Pagume rejects day 6 in non-leap year
        try {
            EthiopianDate(2016, 13, 6)
            fail("Expected exception for Pagume day 6 in non-leap year 2016")
        } catch (e: IllegalArgumentException) {
            // Expected
        }

        // 7. Test all 13 months bijectivity
        for (m in 1..13) {
            val ethDate = EthiopianDate(2017, m, 15.coerceAtMost(if (m == 13) 5 else 30))
            val greg = EthiopianCalendarConverter.toGregorian(ethDate)
            val convertedBack = EthiopianCalendarConverter.toEthiopian(greg)
            assertEquals(ethDate, convertedBack)
        }
    }

    @Test
    fun testPortableCustomerImageBackupAndRestoreAcrossDevices() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val testRepo = MakiRepository(db, context)

        // 1. Create a simulated local avatar file in internal storage
        val photoDir = java.io.File(context.filesDir, "customer_photos").apply { mkdirs() }
        val testImageFile = java.io.File(photoDir, "test_avatar_${System.currentTimeMillis()}.jpg")
        val sampleBytes = "SAMPLE_JPEG_BINARY_DATA_FOR_TESTING".toByteArray(Charsets.UTF_8)
        testImageFile.writeBytes(sampleBytes)

        // 2. Save customer with photo
        val cId = testRepo.saveCustomer(
            CustomerEntity(
                fullName = "Helen Mesfin",
                phoneNumber = "0944556677",
                photoUri = testImageFile.absolutePath
            )
        )

        // 3. Export backup JSON
        val json = testRepo.exportBackupJson()
        assertTrue("JSON must contain photoBase64 payload", json.contains("\"photoBase64\""))

        // 4. Simulate moving to a DIFFERENT device: Delete the original local file
        testImageFile.delete()
        assertFalse(testImageFile.exists())

        // 5. Restore backup onto 'different device'
        testRepo.restoreBackupJson(json)

        // 6. Verify restored customer record
        val restoredCust = db.customerDao().getCustomerById(cId)
        assertNotNull(restoredCust)
        assertTrue("Restored photoUri must not be blank", restoredCust!!.photoUri.isNotBlank())

        val restoredFile = java.io.File(restoredCust.photoUri)
        assertTrue("Restored photo file must exist on the new target system", restoredFile.exists())
        assertEquals("Restored file contents must match original image bytes", String(sampleBytes), String(restoredFile.readBytes()))
    }

    @Test
    fun testFinancialPrecisionFloatingPointImmunityAndRounding() {
        // Test repeated fractional sales where Double addition produces IEEE 754 drift (e.g. 19.99 + 19.99 + ...)
        val price = BigDecimal("19.99")
        val qty = 333
        val revenue = FinancialCalculator.calculateRevenue(qty, price)
        // 333 * 19.99 = 6656.67 exactly
        assertEquals(BigDecimal("6656.67"), revenue)

        // Profit margin calculation with Half-Up rounding
        // Buy: 15.00, Sell: 22.00, Profit: 7.00. Margin: (7.00 / 22.00) * 100 = 31.81818...% -> 31.8%
        val financials = FinancialCalculator.calculateSaleFinancials(
            quantity = 1,
            purchasePrice = BigDecimal("15.00"),
            sellingPrice = BigDecimal("22.00")
        )
        assertEquals(BigDecimal("7.00"), financials.grossProfit)
        assertEquals(31.8, financials.profitMarginPercent, 0.001)

        // Test inventory value for zero and negative edge cases
        assertEquals(BigDecimal.ZERO, FinancialCalculator.calculateInventoryValue(0, BigDecimal("100.00")))
        assertEquals(BigDecimal.ZERO, FinancialCalculator.calculateRevenue(-5, BigDecimal("100.00")))
    }
}
