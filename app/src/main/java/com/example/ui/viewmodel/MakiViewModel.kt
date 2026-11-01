package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MakiDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CelebrationEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.InventoryMovementEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.repository.BackupPreview
import com.example.data.repository.MakiRepository
import com.example.domain.calculator.FinancialCalculator
import com.example.domain.calendar.EthiopianCalendarConverter
import com.example.localization.AmharicStrings
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.localization.EnglishStrings
import com.example.notifications.CelebrationReminderManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class DateFilterType {
    TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, THIS_YEAR, ALL_TIME, CUSTOM
}

data class UpcomingCelebrationItem(
    val celebration: CelebrationEntity,
    val customerName: String,
    val customerPhone: String,
    val nextGregorianDate: LocalDate,
    val daysUntil: Long,
    val lastGiftPurchased: String
)

data class DashboardMetrics(
    val selectedFilterRevenue: BigDecimal = BigDecimal.ZERO,
    val selectedFilterProfit: BigDecimal = BigDecimal.ZERO,
    val selectedFilterSalesCount: Int = 0,
    val todayRevenue: BigDecimal = BigDecimal.ZERO,
    val todayProfit: BigDecimal = BigDecimal.ZERO,
    val todaySalesCount: Int = 0,
    val lifetimeRevenue: BigDecimal = BigDecimal.ZERO,
    val lifetimeProfit: BigDecimal = BigDecimal.ZERO,
    val lifetimeSalesCount: Int = 0,
    val totalProductsCount: Int = 0,
    val totalInventoryStock: Int = 0,
    val totalInventoryCostValue: BigDecimal = BigDecimal.ZERO,
    val lowStockCount: Int = 0
)

data class ProductSalesPerformance(
    val productName: String,
    val totalQuantitySold: Int,
    val totalRevenue: BigDecimal,
    val totalProfit: BigDecimal
)

class MakiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MakiDatabase.getDatabase(application)
    val repository = MakiRepository(db, application)

    // Language State
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    val strings: StateFlow<AppStrings> = _currentLanguage.map { lang ->
        when (lang) {
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.AMHARIC -> AmharicStrings
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EnglishStrings)

    // Events / Toasts / Receipts
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Data streams
    val activeProducts: StateFlow<List<ProductEntity>> = repository.activeProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovements: StateFlow<List<InventoryMovementEntity>> = repository.allMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCustomers: StateFlow<List<CustomerEntity>> = repository.activeCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val celebrations: StateFlow<List<CelebrationEntity>> = repository.activeCelebrations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Timezone-safe epoch millis to LocalDate conversion
    private fun toLocalDate(ts: Long): LocalDate =
        java.time.Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

    // Date Filter State
    private val _selectedDateFilter = MutableStateFlow(DateFilterType.TODAY)
    val selectedDateFilter: StateFlow<DateFilterType> = _selectedDateFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)
    val customDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _customDateRange.asStateFlow()

    init {
        // Load initial language preference
        viewModelScope.launch {
            val langCode = repository.getSetting("app_language") ?: "en"
            _currentLanguage.value = if (langCode == "am") AppLanguage.AMHARIC else AppLanguage.ENGLISH
            // Check notifications on launch
            CelebrationReminderManager.checkAndNotifyCelebrations(getApplication())
        }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            _currentLanguage.value = lang
            repository.setSetting("app_language", lang.code)
        }
    }

    fun setDateFilter(filter: DateFilterType, customRange: Pair<LocalDate, LocalDate>? = null) {
        _selectedDateFilter.value = filter
        if (filter == DateFilterType.CUSTOM && customRange != null) {
            _customDateRange.value = customRange
        }
    }

    // Helper to calculate epoch timestamps for date filters
    private fun getFilterTimestampRange(
        filter: DateFilterType,
        customRange: Pair<LocalDate, LocalDate>?
    ): Pair<Long, Long>? {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        return when (filter) {
            DateFilterType.TODAY -> {
                val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(start, end)
            }
            DateFilterType.YESTERDAY -> {
                val yesterday = today.minusDays(1)
                val start = yesterday.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(start, end)
            }
            DateFilterType.THIS_WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val start = startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = endOfWeek.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(start, end)
            }
            DateFilterType.THIS_MONTH -> {
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                val start = startOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = endOfMonth.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(start, end)
            }
            DateFilterType.LAST_3_MONTHS -> {
                val startOf3M = today.minusMonths(3)
                val start = startOf3M.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(start, end)
            }
            DateFilterType.LAST_6_MONTHS -> {
                val startOf6M = today.minusMonths(6)
                val start = startOf6M.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(start, end)
            }
            DateFilterType.THIS_YEAR -> {
                val startOfYear = today.withDayOfYear(1)
                val endOfYear = today.with(TemporalAdjusters.lastDayOfYear())
                val start = startOfYear.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = endOfYear.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                Pair(start, end)
            }
            DateFilterType.CUSTOM -> {
                if (customRange != null) {
                    val start = customRange.first.atStartOfDay(zone).toInstant().toEpochMilli()
                    val end = customRange.second.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                    Pair(start, end)
                } else null
            }
            DateFilterType.ALL_TIME -> null
        }
    }

    // Filtered Sales according to Date Filter
    val filteredSales: StateFlow<List<SaleEntity>> = combine(
        allSales,
        selectedDateFilter,
        customDateRange
    ) { sales, filter, custom ->
        val today = LocalDate.now()

        sales.filter { sale ->
            val saleDate = toLocalDate(sale.saleTimestamp)
            when (filter) {
                DateFilterType.TODAY -> saleDate == today
                DateFilterType.YESTERDAY -> saleDate == today.minusDays(1)
                DateFilterType.THIS_WEEK -> {
                    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    !saleDate.isBefore(startOfWeek) && !saleDate.isAfter(endOfWeek)
                }
                DateFilterType.THIS_MONTH -> {
                    saleDate.year == today.year && saleDate.month == today.month
                }
                DateFilterType.LAST_3_MONTHS -> {
                    val start = today.minusMonths(3)
                    !saleDate.isBefore(start) && !saleDate.isAfter(today)
                }
                DateFilterType.LAST_6_MONTHS -> {
                    val start = today.minusMonths(6)
                    !saleDate.isBefore(start) && !saleDate.isAfter(today)
                }
                DateFilterType.THIS_YEAR -> {
                    saleDate.year == today.year
                }
                DateFilterType.ALL_TIME -> true
                DateFilterType.CUSTOM -> {
                    if (custom != null) {
                        !saleDate.isBefore(custom.first) && !saleDate.isAfter(custom.second)
                    } else true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filterSalesSummary = combine(
        selectedDateFilter,
        customDateRange
    ) { filter, custom ->
        getFilterTimestampRange(filter, custom)
    }.flatMapLatest { range ->
        if (range == null) {
            repository.lifetimeSalesSummary
        } else {
            repository.getSalesSummaryBetween(range.first, range.second)
        }
    }

    private val todaySalesSummary = run {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        repository.getSalesSummaryBetween(todayStart, todayEnd)
    }

    // Comprehensive Dashboard Metrics (Aggregated directly in SQLite for 10-year scalability)
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        repository.inventorySummary,
        repository.lifetimeSalesSummary,
        todaySalesSummary,
        filterSalesSummary
    ) { inv, lifetime, today, filterSummary ->
        DashboardMetrics(
            selectedFilterRevenue = BigDecimal.valueOf(filterSummary.totalRevenue),
            selectedFilterProfit = BigDecimal.valueOf(filterSummary.totalGrossProfit),
            selectedFilterSalesCount = filterSummary.salesCount,
            todayRevenue = BigDecimal.valueOf(today.totalRevenue),
            todayProfit = BigDecimal.valueOf(today.totalGrossProfit),
            todaySalesCount = today.salesCount,
            lifetimeRevenue = BigDecimal.valueOf(lifetime.totalRevenue),
            lifetimeProfit = BigDecimal.valueOf(lifetime.totalGrossProfit),
            lifetimeSalesCount = lifetime.salesCount,
            totalProductsCount = inv.totalProductsCount,
            totalInventoryStock = inv.totalStockQuantity,
            totalInventoryCostValue = BigDecimal.valueOf(inv.totalCostValue),
            lowStockCount = inv.lowStockCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Best-Selling Products Performance (Aggregated via SQLite compound index)
    val productPerformances: StateFlow<List<ProductSalesPerformance>> = repository.topSellingProducts.map { rows ->
        rows.map { row ->
            ProductSalesPerformance(
                productName = row.productName,
                totalQuantitySold = row.totalQuantitySold,
                totalRevenue = BigDecimal.valueOf(row.totalRevenue),
                totalProfit = BigDecimal.valueOf(row.totalProfit)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upcoming Celebrations within 14 days
    val upcomingCelebrationItems: StateFlow<List<UpcomingCelebrationItem>> = combine(
        celebrations,
        customers,
        repository.recentSales
    ) { cels, custs, sales ->
        val today = LocalDate.now()
        val custMap = custs.associateBy { it.id }

        cels.mapNotNull { cel ->
            val cust = custMap[cel.customerId]
            val nextDate = EthiopianCalendarConverter.getNextCelebrationDate(
                fromDate = today,
                ethMonth = cel.ethMonth,
                ethDay = cel.ethDay,
                recurrenceCycle = cel.recurrenceCycle
            )
            val daysUntil = ChronoUnit.DAYS.between(today, nextDate)

            // Only return celebrations coming in <= 14 days for the upcoming list
            if (daysUntil in 0..14) {
                val customerSales = sales.filter { it.customerId == cel.customerId && !it.isVoided }
                val lastGift = customerSales.firstOrNull()?.productNameSnapshot ?: "Special Gift"
                UpcomingCelebrationItem(
                    celebration = cel,
                    customerName = cust?.fullName ?: "Customer",
                    customerPhone = cust?.phoneNumber ?: "",
                    nextGregorianDate = nextDate,
                    daysUntil = daysUntil,
                    lastGiftPurchased = lastGift
                )
            } else null
        }.sortedBy { it.daysUntil }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun recordSale(
        productId: Long,
        quantity: Int,
        customerId: Long?,
        notes: String,
        onSuccess: (SaleEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sale = repository.recordSale(
                    productId = productId,
                    quantity = quantity,
                    customerId = customerId,
                    notes = notes
                )
                _userMessage.emit("Sale confirmed: ${sale.quantity}x ${sale.productNameSnapshot}")
                onSuccess(sale)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Failed to record sale"
                _userMessage.emit(msg)
                onError(msg)
            }
        }
    }

    fun voidSale(saleId: Long, reason: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.voidSale(saleId, reason)
                _userMessage.emit("Sale voided and inventory restored successfully.")
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to void sale")
            }
        }
    }

    fun saveProduct(
        id: Long = 0,
        name: String,
        category: String,
        purchasePrice: Double,
        sellingPrice: Double,
        stockQuantity: Int,
        lowStockThreshold: Int = 3,
        description: String = "",
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _userMessage.emit("Product name cannot be empty")
                    return@launch
                }
                if (purchasePrice < 0 || sellingPrice < 0) {
                    _userMessage.emit("Prices cannot be negative")
                    return@launch
                }
                if (stockQuantity < 0) {
                    _userMessage.emit("Stock quantity cannot be negative")
                    return@launch
                }
                if (lowStockThreshold < 0) {
                    _userMessage.emit("Low stock threshold cannot be negative")
                    return@launch
                }
                val product = ProductEntity(
                    id = id,
                    name = name.trim(),
                    category = category.trim(),
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice,
                    stockQuantity = stockQuantity,
                    lowStockThreshold = lowStockThreshold,
                    description = description.trim()
                )
                repository.saveProduct(product)
                _userMessage.emit("Product saved: $name")
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to save product")
            }
        }
    }

    fun archiveProduct(id: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.archiveProduct(id)
                _userMessage.emit("Product archived.")
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to archive product")
            }
        }
    }

    fun restockProduct(productId: Long, quantityToAdd: Int, reason: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.restockProduct(productId, quantityToAdd, reason)
                _userMessage.emit("Stock added successfully.")
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to add stock")
            }
        }
    }

    fun adjustInventory(productId: Long, delta: Int, isDamage: Boolean, reason: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.adjustInventory(productId, delta, isDamage, reason)
                _userMessage.emit("Inventory adjusted successfully.")
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to adjust inventory")
            }
        }
    }

    fun saveCustomer(
        id: Long = 0,
        name: String,
        phone: String,
        alternativePhone: String = "",
        photoUri: String = "",
        gender: String = "",
        address: String = "",
        city: String = "Addis Ababa",
        notes: String = "",
        giftPreferences: String = "",
        preferredLanguage: String = "English",
        preferredContactMethod: String = "WhatsApp",
        marketingConsent: Boolean = true,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _userMessage.emit("Customer name cannot be empty")
                    return@launch
                }
                if (phone.isBlank()) {
                    _userMessage.emit("Phone number cannot be empty")
                    return@launch
                }
                val cust = CustomerEntity(
                    id = id,
                    fullName = name.trim(),
                    phoneNumber = phone.trim(),
                    alternativePhone = alternativePhone.trim(),
                    photoUri = photoUri.trim(),
                    gender = gender.trim(),
                    address = address.trim(),
                    city = city.trim(),
                    notes = notes.trim(),
                    giftPreferences = giftPreferences.trim(),
                    preferredLanguage = preferredLanguage,
                    preferredContactMethod = preferredContactMethod,
                    marketingConsent = marketingConsent,
                    isArchived = false
                )
                val newId = repository.saveCustomer(cust)
                _userMessage.emit("Customer saved: $name")
                onComplete(newId)
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to save customer")
            }
        }
    }

    fun archiveCustomer(id: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.archiveCustomer(id)
                _userMessage.emit("Customer archived.")
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to archive customer")
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer)
                _userMessage.emit("Customer deleted.")
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to delete customer")
            }
        }
    }

    fun saveCelebration(
        id: Long = 0,
        customerId: Long,
        name: String,
        personCelebrated: String,
        ethMonth: Int,
        ethDay: Int,
        recurrenceCycle: String = "YEARLY",
        reminderDaysBefore: Int = 7,
        notes: String = "",
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _userMessage.emit("Celebration name cannot be empty")
                    return@launch
                }
                if (ethMonth !in 1..13) {
                    _userMessage.emit("Ethiopian month must be between 1 and 13")
                    return@launch
                }
                if (ethDay !in 1..30) {
                    _userMessage.emit("Ethiopian day must be between 1 and 30")
                    return@launch
                }
                val cel = CelebrationEntity(
                    id = id,
                    customerId = customerId,
                    celebrationName = name.trim(),
                    personCelebrated = personCelebrated.trim(),
                    ethMonth = ethMonth,
                    ethDay = ethDay,
                    recurrenceCycle = recurrenceCycle,
                    reminderDaysBefore = reminderDaysBefore,
                    notes = notes.trim()
                )
                repository.saveCelebration(cel)
                _userMessage.emit("Celebration saved.")
                CelebrationReminderManager.checkAndNotifyCelebrations(getApplication())
                onComplete()
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to save celebration")
            }
        }
    }

    fun deleteCelebration(celebration: CelebrationEntity) {
        viewModelScope.launch {
            try {
                repository.deleteCelebration(celebration)
                _userMessage.emit("Celebration deleted.")
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to delete celebration")
            }
        }
    }

    fun markCelebrationNotified(celebrationId: Long) {
        viewModelScope.launch {
            repository.markCelebrationNotified(celebrationId)
            _userMessage.emit("Marked as contacted.")
        }
    }

    suspend fun exportBackupJson(): String {
        return repository.exportBackupJson()
    }

    suspend fun parseBackupPreview(json: String): BackupPreview {
        return repository.parseBackupPreview(json)
    }

    fun restoreBackup(json: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.restoreBackupJson(json)
                _userMessage.emit("Database restored successfully.")
                CelebrationReminderManager.checkAndNotifyCelebrations(getApplication())
                onComplete(true, "Restoration complete!")
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Failed to restore backup"
                _userMessage.emit(err)
                onComplete(false, err)
            }
        }
    }
}
