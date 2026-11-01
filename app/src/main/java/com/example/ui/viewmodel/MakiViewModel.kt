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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL_TIME, CUSTOM
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
    val repository = MakiRepository(db)

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

    val celebrations: StateFlow<List<CelebrationEntity>> = repository.activeCelebrations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Filtered Sales according to Date Filter
    val filteredSales: StateFlow<List<SaleEntity>> = combine(
        allSales,
        selectedDateFilter,
        customDateRange
    ) { sales, filter, custom ->
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        fun toLocalDate(ts: Long) = LocalDate.ofEpochDay(ts / (1000 * 60 * 60 * 24))

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

    // Comprehensive Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allProducts,
        allSales,
        filteredSales
    ) { products, sales, filtered ->
        val validSales = sales.filter { !it.isVoided }
        val validFiltered = filtered.filter { !it.isVoided }
        val today = LocalDate.now()

        val todaySales = validSales.filter {
            LocalDate.ofEpochDay(it.saleTimestamp / (1000 * 60 * 60 * 24)) == today
        }

        // Today metrics
        var todayRev = BigDecimal.ZERO
        var todayProf = BigDecimal.ZERO
        todaySales.forEach {
            todayRev = todayRev.add(BigDecimal.valueOf(it.totalRevenue))
            todayProf = todayProf.add(BigDecimal.valueOf(it.grossProfit))
        }

        // Selected filter metrics
        var filterRev = BigDecimal.ZERO
        var filterProf = BigDecimal.ZERO
        validFiltered.forEach {
            filterRev = filterRev.add(BigDecimal.valueOf(it.totalRevenue))
            filterProf = filterProf.add(BigDecimal.valueOf(it.grossProfit))
        }

        // Lifetime metrics
        var lifeRev = BigDecimal.ZERO
        var lifeProf = BigDecimal.ZERO
        validSales.forEach {
            lifeRev = lifeRev.add(BigDecimal.valueOf(it.totalRevenue))
            lifeProf = lifeProf.add(BigDecimal.valueOf(it.grossProfit))
        }

        // Inventory values
        val activeProds = products.filter { !it.isArchived }
        var totalStock = 0
        var inventoryCostVal = BigDecimal.ZERO
        var lowStockCount = 0

        activeProds.forEach { p ->
            totalStock += p.stockQuantity
            inventoryCostVal = inventoryCostVal.add(
                FinancialCalculator.calculateInventoryValue(p.stockQuantity, BigDecimal.valueOf(p.purchasePrice))
            )
            if (p.stockQuantity <= p.lowStockThreshold) {
                lowStockCount++
            }
        }

        DashboardMetrics(
            selectedFilterRevenue = filterRev,
            selectedFilterProfit = filterProf,
            selectedFilterSalesCount = validFiltered.size,
            todayRevenue = todayRev,
            todayProfit = todayProf,
            todaySalesCount = todaySales.size,
            lifetimeRevenue = lifeRev,
            lifetimeProfit = lifeProf,
            lifetimeSalesCount = validSales.size,
            totalProductsCount = activeProds.size,
            totalInventoryStock = totalStock,
            totalInventoryCostValue = inventoryCostVal,
            lowStockCount = lowStockCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Best-Selling Products Performance
    val productPerformances: StateFlow<List<ProductSalesPerformance>> = allSales.map { sales ->
        val nonVoided = sales.filter { !it.isVoided }
        nonVoided.groupBy { it.productNameSnapshot }
            .map { (name, groupSales) ->
                var qty = 0
                var rev = BigDecimal.ZERO
                var prof = BigDecimal.ZERO
                groupSales.forEach { s ->
                    qty += s.quantity
                    rev = rev.add(BigDecimal.valueOf(s.totalRevenue))
                    prof = prof.add(BigDecimal.valueOf(s.grossProfit))
                }
                ProductSalesPerformance(
                    productName = name,
                    totalQuantitySold = qty,
                    totalRevenue = rev,
                    totalProfit = prof
                )
            }.sortedByDescending { it.totalQuantitySold }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upcoming Celebrations within 7 days
    val upcomingCelebrationItems: StateFlow<List<UpcomingCelebrationItem>> = combine(
        celebrations,
        customers,
        allSales
    ) { cels, custs, sales ->
        val today = LocalDate.now()
        val custMap = custs.associateBy { it.id }

        cels.mapNotNull { cel ->
            val cust = custMap[cel.customerId]
            val nextDate = EthiopianCalendarConverter.getNextCelebrationDate(
                fromDate = today,
                ethMonth = cel.ethMonth,
                ethDay = cel.ethDay
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
        notes: String = "",
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cust = CustomerEntity(
                    id = id,
                    fullName = name.trim(),
                    phoneNumber = phone.trim(),
                    notes = notes.trim()
                )
                val newId = repository.saveCustomer(cust)
                _userMessage.emit("Customer saved: $name")
                onComplete(newId)
            } catch (e: Exception) {
                _userMessage.emit(e.localizedMessage ?: "Failed to save customer")
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
