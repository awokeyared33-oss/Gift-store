package com.example

import com.example.domain.calculator.FinancialCalculator
import com.example.domain.calendar.EthiopianCalendarConverter
import com.example.domain.calendar.EthiopianDate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MakiBusinessLogicTest {

    @Test
    fun `test exact sales calculation from specification`() {
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
    fun `test selling at loss warning`() {
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
    fun `test inventory cost value calculation`() {
        val qty = 25
        val buyPrice = BigDecimal("120.50")
        val totalValue = FinancialCalculator.calculateInventoryValue(qty, buyPrice)

        assertEquals(BigDecimal("3012.50"), totalValue)
    }

    @Test
    fun `test ethiopian calendar new year conversion`() {
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
    fun `test ethiopian calendar pagume month 13 and leap year`() {
        // Ethiopian leap years occur when year % 4 == 3
        assertTrue(EthiopianDate.isEthiopianLeapYear(2015)) // 2015 % 4 == 3
        assertFalse(EthiopianDate.isEthiopianLeapYear(2016)) // 2016 % 4 == 0
        assertFalse(EthiopianDate.isEthiopianLeapYear(2017)) // 2017 % 4 == 1

        // Pagume 5 in standard year
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
    fun `test recurring celebration next occurrence calculation`() {
        // Customer Abebe's mother's birthday on Meskerem 12
        val referenceToday = LocalDate.of(2024, 9, 1) // Before Meskerem 12
        val nextOccurrence = EthiopianCalendarConverter.getNextCelebrationDate(
            fromDate = referenceToday,
            ethMonth = 1,
            ethDay = 12
        )

        val nextEth = EthiopianCalendarConverter.toEthiopian(nextOccurrence)
        assertEquals(1, nextEth.month)
        assertEquals(12, nextEth.day)
        assertTrue(!nextOccurrence.isBefore(referenceToday))
    }

    @Test
    fun `test backup json structure and parsing`() {
        val root = JSONObject()
        root.put("schema_version", 1)
        root.put("app_name", "MAKI BRANDS")
        root.put("app_version", "1.0.0")

        assertEquals(1, root.getInt("schema_version"))
        assertEquals("MAKI BRANDS", root.getString("app_name"))
    }
}
