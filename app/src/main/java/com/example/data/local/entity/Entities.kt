package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["isArchived"]),
        Index(value = ["isArchived", "name"]),
        Index(value = ["isArchived", "stockQuantity", "lowStockThreshold"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val purchasePrice: Double, // in ETB
    val sellingPrice: Double,  // in ETB
    val stockQuantity: Int,
    val lowStockThreshold: Int = 3,
    val description: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "gift"
)

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["saleTimestamp"]),
        Index(value = ["productId"]),
        Index(value = ["customerId"]),
        Index(value = ["isVoided"]),
        Index(value = ["isVoided", "saleTimestamp"]),
        Index(value = ["customerId", "isVoided", "saleTimestamp"]),
        Index(value = ["isVoided", "productNameSnapshot"])
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productNameSnapshot: String,
    val quantity: Int,
    val purchasePriceSnapshot: Double, // immutable historical buy price
    val sellingPriceSnapshot: Double,  // immutable historical sell price
    val totalRevenue: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val saleTimestamp: Long = System.currentTimeMillis(),
    val ethiopianDateStr: String,      // formatted e.g. "Meskerem 12, 2017"
    val customerId: Long? = null,
    val customerNameSnapshot: String? = null,
    val notes: String = "",
    val isVoided: Boolean = false,
    val voidReason: String = "",
    val voidTimestamp: Long? = null
)

@Entity(
    tableName = "inventory_movements",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["timestamp"]),
        Index(value = ["productId", "timestamp"])
    ]
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productNameSnapshot: String,
    val movementType: String, // "PURCHASE_RESTOCK", "SALE_DEDUCTION", "DAMAGE_LOSS", "MANUAL_ADJUSTMENT", "SALE_VOID_RESTORE"
    val quantityDelta: Int,   // positive or negative
    val previousQuantity: Int,
    val newQuantity: Int,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val ethiopianDateStr: String
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["fullName"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["isArchived"]),
        Index(value = ["isArchived", "fullName"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val phoneNumber: String,
    val alternativePhone: String = "",
    val photoUri: String = "",
    val gender: String = "",
    val address: String = "",
    val city: String = "Addis Ababa",
    val notes: String = "",
    val giftPreferences: String = "",
    val preferredLanguage: String = "English",
    val preferredContactMethod: String = "WhatsApp",
    val marketingConsent: Boolean = true,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "celebrations",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["isActive"]),
        Index(value = ["isActive", "ethMonth", "ethDay"]),
        Index(value = ["customerId", "isActive"])
    ]
)
data class CelebrationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val celebrationName: String,     // e.g. "Mother's Birthday", "Wedding Anniversary"
    val celebrationCategory: String = "Birthday",
    val personCelebrated: String,    // e.g. "Mother", "Wife", "Customer"
    val relationship: String = "Mother",
    val ethMonth: Int,               // 1 to 13 (Ethiopian month)
    val ethDay: Int,                 // 1 to 30 (or 1 to 5/6 for Pagume)
    val recurrenceCycle: String = "YEARLY", // "YEARLY", "MONTHLY", "NONE"
    val reminderDaysBefore: Int = 7, // 7 days default
    val notes: String = "",
    val isActive: Boolean = true,
    val lastNotifiedTimestamp: Long? = null
)

@Entity(
    tableName = "previous_gifts",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["customerId", "createdAt"])
    ]
)
data class PreviousGiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val giftItemName: String,
    val purchaseDateGregorian: String = "",
    val purchaseDateEthiopian: String = "",
    val quantity: Int = 1,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val customizationDetails: String = "",
    val recipient: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
