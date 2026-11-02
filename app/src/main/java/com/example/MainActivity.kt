package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CustomerEntity
import com.example.notifications.CelebrationReminderManager
import com.example.ui.backup.BackupRestoreScreen
import com.example.ui.celebrations.CelebrationsScreen
import com.example.ui.customers.CustomerProfileScreen
import com.example.ui.customers.CustomerRegistrationScreen
import com.example.ui.customers.CustomersScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.history.SalesHistoryScreen
import com.example.ui.more.MoreScreen
import com.example.ui.products.ProductsScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.sales.SalesScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.MakiSplashScreen
import com.example.ui.theme.MakiBrandsTheme
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.viewmodel.MakiViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen {
    DASHBOARD,
    PRODUCTS,
    SALES,
    CUSTOMERS,
    CUSTOMER_REGISTRATION,
    CUSTOMER_PROFILE,
    MORE,
    SALES_HISTORY,
    CELEBRATIONS,
    REPORTS,
    BACKUP,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MakiViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CelebrationReminderManager.schedulePeriodicReminderCheck(this)
        CelebrationReminderManager.checkAndNotifyCelebrations(this)

        setContent {
            MakiBrandsTheme {
                val context = LocalContext.current
                val strings by viewModel.strings.collectAsStateWithLifecycle()
                var showSplash by rememberSaveable { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
                var selectedCustomerForProfile by remember { mutableStateOf<CustomerEntity?>(null) }
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "SplashTransition"
                ) { isSplashActive ->
                    if (isSplashActive) {
                        MakiSplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        // Android 13+ Notification Permission Request
                        val notificationPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            if (isGranted) {
                                CelebrationReminderManager.checkAndNotifyCelebrations(context)
                            }
                        }

                        LaunchedEffect(Unit) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }

                        // Listen to ViewModel user messages & toasts
                        LaunchedEffect(Unit) {
                            viewModel.userMessage.collectLatest { msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        }

                        // Handle Back press for sub-screens
                        when (currentScreen) {
                            Screen.CUSTOMER_REGISTRATION, Screen.CUSTOMER_PROFILE -> {
                                BackHandler { currentScreen = Screen.CUSTOMERS }
                            }
                            Screen.SALES_HISTORY, Screen.CELEBRATIONS, Screen.REPORTS, Screen.BACKUP, Screen.SETTINGS -> {
                                BackHandler { currentScreen = Screen.MORE }
                            }
                            Screen.PRODUCTS, Screen.SALES, Screen.CUSTOMERS, Screen.MORE -> {
                                BackHandler { currentScreen = Screen.DASHBOARD }
                            }
                            else -> {}
                        }

                        val isSubScreen = currentScreen in listOf(
                            Screen.CUSTOMER_REGISTRATION,
                            Screen.CUSTOMER_PROFILE,
                            Screen.SALES_HISTORY,
                            Screen.CELEBRATIONS,
                            Screen.REPORTS,
                            Screen.BACKUP,
                            Screen.SETTINGS
                        )

                        Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (isSubScreen && currentScreen != Screen.CUSTOMER_REGISTRATION && currentScreen != Screen.CUSTOMER_PROFILE) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (currentScreen) {
                                            Screen.SALES_HISTORY -> strings.salesHistory
                                            Screen.CELEBRATIONS -> strings.celebrations
                                            Screen.REPORTS -> strings.reports
                                            Screen.BACKUP -> strings.backupRestore
                                            Screen.SETTINGS -> strings.settings
                                            else -> ""
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MakiTextDark
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { currentScreen = Screen.MORE },
                                        modifier = Modifier.testTag("top_bar_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = MakiPrimaryBlue
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.White
                                )
                            )
                        }
                    },
                    bottomBar = {
                        val showBottomNav = currentScreen in listOf(
                            Screen.DASHBOARD,
                            Screen.PRODUCTS,
                            Screen.SALES,
                            Screen.CUSTOMERS,
                            Screen.MORE,
                            Screen.CUSTOMER_REGISTRATION
                        )
                        if (showBottomNav) {
                            NavigationBar(
                                containerColor = Color.White,
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .testTag("main_bottom_nav")
                                    .border(width = 1.dp, color = com.example.ui.theme.MakiSurfaceBorder)
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == Screen.DASHBOARD,
                                    onClick = { currentScreen = Screen.DASHBOARD },
                                    icon = { Icon(androidx.compose.material.icons.Icons.Default.Home, contentDescription = strings.dashboard) },
                                    label = { Text(if (strings.dashboard == "Dashboard") "Home" else strings.dashboard, fontSize = 11.sp, fontWeight = if (currentScreen == Screen.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MakiPrimaryBlue,
                                        selectedTextColor = MakiPrimaryBlue,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MakiTextSecondary,
                                        unselectedTextColor = MakiTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_dashboard")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.PRODUCTS,
                                    onClick = { currentScreen = Screen.PRODUCTS },
                                    icon = { Icon(androidx.compose.material.icons.Icons.Default.CardGiftcard, contentDescription = strings.products) },
                                    label = { Text(strings.products, fontSize = 11.sp, fontWeight = if (currentScreen == Screen.PRODUCTS) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MakiPrimaryBlue,
                                        selectedTextColor = MakiPrimaryBlue,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MakiTextSecondary,
                                        unselectedTextColor = MakiTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_products")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.SALES,
                                    onClick = { currentScreen = Screen.SALES },
                                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = strings.sales) },
                                    label = { Text(strings.sales, fontSize = 11.sp, fontWeight = if (currentScreen == Screen.SALES) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MakiPrimaryBlue,
                                        selectedTextColor = MakiPrimaryBlue,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MakiTextSecondary,
                                        unselectedTextColor = MakiTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_sales")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.CUSTOMERS || currentScreen == Screen.CUSTOMER_REGISTRATION,
                                    onClick = { currentScreen = Screen.CUSTOMERS },
                                    icon = { Icon(Icons.Default.People, contentDescription = strings.customers) },
                                    label = { Text(strings.customers, fontSize = 11.sp, fontWeight = if (currentScreen == Screen.CUSTOMERS || currentScreen == Screen.CUSTOMER_REGISTRATION) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MakiPrimaryBlue,
                                        selectedTextColor = MakiPrimaryBlue,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MakiTextSecondary,
                                        unselectedTextColor = MakiTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_customers")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == Screen.MORE,
                                    onClick = { currentScreen = Screen.MORE },
                                    icon = { Icon(androidx.compose.material.icons.Icons.Default.MoreHoriz, contentDescription = strings.more) },
                                    label = { Text(strings.more, fontSize = 11.sp, fontWeight = if (currentScreen == Screen.MORE) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MakiPrimaryBlue,
                                        selectedTextColor = MakiPrimaryBlue,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MakiTextSecondary,
                                        unselectedTextColor = MakiTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_more")
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            Screen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToSales = { currentScreen = Screen.SALES },
                                onNavigateToProducts = { currentScreen = Screen.PRODUCTS },
                                onNavigateToCustomers = { currentScreen = Screen.CUSTOMERS }
                            )

                            Screen.PRODUCTS -> ProductsScreen(viewModel = viewModel)

                            Screen.SALES -> SalesScreen(
                                viewModel = viewModel,
                                onNavigateToHistory = { currentScreen = Screen.SALES_HISTORY }
                            )

                            Screen.CUSTOMERS -> CustomersScreen(
                                viewModel = viewModel,
                                onNavigateToRegistration = { currentScreen = Screen.CUSTOMER_REGISTRATION },
                                onNavigateToProfile = { cust ->
                                    selectedCustomerForProfile = cust
                                    currentScreen = Screen.CUSTOMER_PROFILE
                                }
                            )

                            Screen.CUSTOMER_REGISTRATION -> CustomerRegistrationScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = Screen.CUSTOMERS },
                                onCustomerSaved = { _ ->
                                    currentScreen = Screen.CUSTOMERS
                                }
                            )

                            Screen.CUSTOMER_PROFILE -> {
                                val cust = selectedCustomerForProfile
                                if (cust != null) {
                                    CustomerProfileScreen(
                                        customer = cust,
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreen = Screen.CUSTOMERS },
                                        onAddCelebration = {
                                            currentScreen = Screen.CUSTOMERS
                                        }
                                    )
                                } else {
                                    currentScreen = Screen.CUSTOMERS
                                }
                            }

                            Screen.MORE -> MoreScreen(
                                viewModel = viewModel,
                                onNavigateToHistory = { currentScreen = Screen.SALES_HISTORY },
                                onNavigateToCelebrations = { currentScreen = Screen.CELEBRATIONS },
                                onNavigateToReports = { currentScreen = Screen.REPORTS },
                                onNavigateToBackup = { currentScreen = Screen.BACKUP },
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                            )

                            Screen.SALES_HISTORY -> SalesHistoryScreen(viewModel = viewModel)

                            Screen.CELEBRATIONS -> CelebrationsScreen(viewModel = viewModel)

                            Screen.REPORTS -> ReportsScreen(viewModel = viewModel)

                            Screen.BACKUP -> BackupRestoreScreen(viewModel = viewModel)

                            Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
                    }
                }
            }
        }
    }
}
