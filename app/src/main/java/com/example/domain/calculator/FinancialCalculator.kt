package com.example.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Decimal-safe financial and inventory calculation engine for MAKI BRANDS.
 * Uses BigDecimal internally to eliminate floating-point precision loss.
 */
object FinancialCalculator {

    const val CURRENCY_CODE = "ETB"

    fun calculateRevenue(quantity: Int, sellingPrice: BigDecimal): BigDecimal {
        if (quantity <= 0) return BigDecimal.ZERO
        return sellingPrice.multiply(BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateCost(quantity: Int, purchasePrice: BigDecimal): BigDecimal {
        if (quantity <= 0) return BigDecimal.ZERO
        return purchasePrice.multiply(BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateGrossProfit(revenue: BigDecimal, cost: BigDecimal): BigDecimal {
        return revenue.subtract(cost).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateSaleFinancials(
        quantity: Int,
        purchasePrice: BigDecimal,
        sellingPrice: BigDecimal
    ): SaleFinancials {
        val revenue = calculateRevenue(quantity, sellingPrice)
        val cost = calculateCost(quantity, purchasePrice)
        val grossProfit = calculateGrossProfit(revenue, cost)
        val profitMarginPercent = if (revenue > BigDecimal.ZERO) {
            grossProfit.multiply(BigDecimal(100))
                .divide(revenue, 1, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }
        val isSellingAtLoss = sellingPrice < purchasePrice
        return SaleFinancials(
            quantity = quantity,
            purchasePrice = purchasePrice.setScale(2, RoundingMode.HALF_UP),
            sellingPrice = sellingPrice.setScale(2, RoundingMode.HALF_UP),
            revenue = revenue,
            cost = cost,
            grossProfit = grossProfit,
            profitMarginPercent = profitMarginPercent,
            isSellingAtLoss = isSellingAtLoss
        )
    }

    fun calculateInventoryValue(quantity: Int, purchasePrice: BigDecimal): BigDecimal {
        if (quantity <= 0) return BigDecimal.ZERO
        return purchasePrice.multiply(BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP)
    }

    fun formatBirr(amount: BigDecimal): String {
        return formatMoney(amount)
    }

    fun formatBirr(amount: Double): String {
        return formatMoney(BigDecimal.valueOf(amount))
    }

    fun formatMoney(amount: BigDecimal): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return "${formatter.format(amount)} $CURRENCY_CODE"
    }

    fun formatMoneyWithoutCurrency(amount: BigDecimal): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }
}

data class SaleFinancials(
    val quantity: Int,
    val purchasePrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val grossProfit: BigDecimal,
    val profitMarginPercent: Double,
    val isSellingAtLoss: Boolean
)
