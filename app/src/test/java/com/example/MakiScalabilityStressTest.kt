package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MakiDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.repository.MakiRepository
import com.example.domain.calculator.FinancialCalculator
import com.example.domain.calendar.EthiopianCalendarConverter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MakiScalabilityStressTest {

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
    fun testTenYearEthiopianRecurrenceCalculations() {
        // Test celebration recurrence across 10 continuous years (2025 to 2035)
        var currentDate = LocalDate.of(2025, 1, 1)
        val targetEthMonth = 1 // Meskerem
        val targetEthDay = 1   // Enkutatash (Ethiopian New Year)

        for (year in 2025..2035) {
            val nextDate = EthiopianCalendarConverter.getNextCelebrationDate(
                fromDate = currentDate,
                ethMonth = targetEthMonth,
                ethDay = targetEthDay,
                recurrenceCycle = "ANNUAL"
            )

            assertTrue(
                "Celebration date must not be before fromDate ($currentDate vs $nextDate)",
                !nextDate.isBefore(currentDate)
            )
            // Ethiopian New Year Meskerem 1 is always Sept 11 or Sept 12
            assertEquals(9, nextDate.monthValue)
            assertTrue(nextDate.dayOfMonth == 11 || nextDate.dayOfMonth == 12)

            // Step forward to after this celebration
            currentDate = nextDate.plusDays(1)
        }
    }

    @Test
    fun testHighVolumeSalesDatabaseScalabilityAndAggregations() = runBlocking {
        // Insert products
        val productCount = 100
        val products = (1..productCount).map { i ->
            ProductEntity(
                id = i.toLong(),
                name = "Luxury Gift Item #$i",
                category = if (i % 2 == 0) "Chocolates" else "Gift Boxes",
                purchasePrice = 100.0,
                sellingPrice = 150.0,
                stockQuantity = 500,
                lowStockThreshold = 10,
                description = "High quality luxury gift item number $i",
                isArchived = false
            )
        }
        db.productDao().insertAll(products)

        // Bulk insert 2,000 sales transactions simulating multi-year business data
        val saleCount = 2000
        val baseTimestamp = 1700000000000L
        val sales = (1..saleCount).map { i ->
            SaleEntity(
                id = i.toLong(),
                productId = (1 + (i % productCount)).toLong(),
                productNameSnapshot = "Luxury Gift Item #${1 + (i % productCount)}",
                quantity = 2,
                purchasePriceSnapshot = 100.0,
                sellingPriceSnapshot = 150.0,
                totalRevenue = 300.0, // 2 * 150.0
                totalCost = 200.0,    // 2 * 100.0
                grossProfit = 100.0,  // 300 - 200
                customerId = if (i % 5 == 0) (i % 20).toLong() else null,
                customerNameSnapshot = if (i % 5 == 0) "VIP Customer #${i % 20}" else null,
                saleTimestamp = baseTimestamp + (i * 3600000L), // hourly intervals
                ethiopianDateStr = "Meskerem 1, 2017",
                notes = "Sale transaction #$i",
                isVoided = (i % 50 == 0), // 40 voided sales
                voidReason = if (i % 50 == 0) "Customer change of mind" else "",
                voidTimestamp = if (i % 50 == 0) baseTimestamp + (i * 3600000L) + 60000L else null
            )
        }

        // Test chunked insert
        sales.chunked(500).forEach { chunk ->
            db.saleDao().insertAll(chunk)
        }

        // Test SQLite aggregation performance and precision
        val lifetimeSummary = repository.lifetimeSalesSummary.first()
        val validSalesCount = saleCount - (saleCount / 50) // 2000 - 40 = 1960
        assertEquals(validSalesCount, lifetimeSummary.salesCount)
        assertEquals(validSalesCount * 300.0, lifetimeSummary.totalRevenue, 0.001)
        assertEquals(validSalesCount * 100.0, lifetimeSummary.totalGrossProfit, 0.001)

        // Test date-ranged aggregation query
        val startTime = baseTimestamp + (100 * 3600000L)
        val endTime = baseTimestamp + (200 * 3600000L)
        val rangeSummary = repository.getSalesSummaryBetween(startTime, endTime).first()
        assertTrue(rangeSummary.salesCount in 98..102)

        // Test top selling products via SQLite index aggregation
        val topProducts = repository.topSellingProducts.first()
        assertTrue("Top products must be populated", topProducts.isNotEmpty())
        assertTrue("Top products limit 10", topProducts.size <= 10)

        // Test inventory summary
        val invSummary = repository.inventorySummary.first()
        assertEquals(100, invSummary.totalProductsCount)
        assertEquals(50000, invSummary.totalStockQuantity)
        assertEquals(50000 * 100.0, invSummary.totalCostValue, 0.01)
        assertEquals(0, invSummary.lowStockCount)
    }

    @Test
    fun testPaginationAndSearchPerformance() = runBlocking {
        // Insert 500 customers
        val customers = (1..500).map { i ->
            CustomerEntity(
                id = i.toLong(),
                fullName = if (i == 250) "Yared Awoke Special" else "Customer Name #$i",
                phoneNumber = "091100" + String.format("%04d", i),
                city = if (i % 2 == 0) "Addis Ababa" else "Hawassa",
                notes = "Valued gift store customer #$i",
                photoUri = "",
                isArchived = false,
                createdAt = System.currentTimeMillis()
            )
        }
        customers.chunked(250).forEach {
            db.customerDao().insertAll(it)
        }

        // Test paged search
        val pagedResult = repository.searchCustomersPaged("Yared", limit = 10, offset = 0)
        assertEquals(1, pagedResult.size)
        assertEquals("Yared Awoke Special", pagedResult.first().fullName)

        // Test paging offset
        val page1 = db.customerDao().getActiveCustomersPaged(limit = 20, offset = 0)
        val page2 = db.customerDao().getActiveCustomersPaged(limit = 20, offset = 20)
        assertEquals(20, page1.size)
        assertEquals(20, page2.size)
        assertTrue("Page 1 and Page 2 must not overlap", page1.intersect(page2.toSet()).isEmpty())
    }

    @Test
    fun testFinancialPrecisionAcrossTenThousandCalculations() {
        var runningTotalRevenue = BigDecimal.ZERO
        var runningTotalProfit = BigDecimal.ZERO

        val unitCost = BigDecimal("149.99")
        val unitPrice = BigDecimal("249.99")
        val qty = 3

        for (i in 1..10000) {
            val rev = FinancialCalculator.calculateRevenue(qty, unitPrice)
            val cost = FinancialCalculator.calculateCost(qty, unitCost)
            val prof = FinancialCalculator.calculateGrossProfit(rev, cost)
            runningTotalRevenue = runningTotalRevenue.add(rev)
            runningTotalProfit = runningTotalProfit.add(prof)
        }

        val expectedSingleRev = BigDecimal("749.97")
        val expectedSingleProf = BigDecimal("300.00")
        assertEquals(expectedSingleRev.multiply(BigDecimal(10000)), runningTotalRevenue)
        assertEquals(expectedSingleProf.multiply(BigDecimal(10000)), runningTotalProfit)
    }

    @Test
    fun testSchemaV4BackupExportAndChunkedRestore() = runBlocking {
        // Create sample product and customer
        val product = ProductEntity(
            id = 1L,
            name = "Handmade Chocolate Box",
            category = "Chocolates",
            purchasePrice = 120.0,
            sellingPrice = 200.0,
            stockQuantity = 50,
            lowStockThreshold = 5,
            description = "Handmade artisan chocolates",
            isArchived = false
        )
        db.productDao().insert(product)

        val customer = CustomerEntity(
            id = 1L,
            fullName = "Abebe Kebede",
            phoneNumber = "0911223344",
            city = "Addis Ababa",
            notes = "Prefers dark chocolate",
            photoUri = "",
            isArchived = false,
            createdAt = System.currentTimeMillis()
        )
        db.customerDao().insert(customer)

        val jsonString = repository.exportBackupJson()
        assertNotNull(jsonString)

        val preview = repository.parseBackupPreview(jsonString)
        assertNotNull(preview)
        assertEquals(4, preview.schemaVersion)
        assertEquals(1, preview.productCount)
        assertEquals(1, preview.customerCount)

        // Clear and restore
        db.productDao().clearAll()
        db.customerDao().clearAll()
        assertEquals(0, db.productDao().getActiveProductsCount().first())

        repository.restoreBackupJson(jsonString)
        assertEquals(1, db.productDao().getActiveProductsCount().first())
        val restoredProduct = db.productDao().getProductById(1L)
        assertNotNull(restoredProduct)
        assertEquals("Handmade Chocolate Box", restoredProduct?.name)
    }
}
