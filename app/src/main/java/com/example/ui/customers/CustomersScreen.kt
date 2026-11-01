package com.example.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CelebrationEntity
import com.example.data.local.entity.CustomerEntity
import com.example.domain.calendar.EthiopianCalendarConverter
import com.example.domain.calendar.EthiopianDate
import com.example.ui.components.EthiopianDatePickerDialog
import com.example.ui.theme.MakiError
import com.example.ui.theme.MakiLightBlue
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.theme.MakiTextTertiary
import com.example.ui.viewmodel.MakiViewModel
import java.time.LocalDate

@Composable
fun CustomersScreen(
    viewModel: MakiViewModel,
    onNavigateToRegistration: () -> Unit = {},
    onNavigateToProfile: (CustomerEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val celebrations by viewModel.celebrations.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var customerToAddCelebration by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery) ||
                    it.city.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // --- 1. HEADER ("Customers" title + Blue Pill "+ Add Customer") ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customers",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MakiTextDark
            )

            Button(
                onClick = onNavigateToRegistration,
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                shape = RoundedCornerShape(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                modifier = Modifier.testTag("customers_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Customer",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. SEARCH BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MakiTextTertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search customers...",
                        fontSize = 14.sp,
                        color = MakiTextTertiary
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MakiTextDark,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customers_search_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. CUSTOMER LIST ---
        if (filteredCustomers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No customers registered yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MakiTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToRegistration,
                        colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Add First Customer")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val customerCelebrations = celebrations.filter { it.customerId == customer.id }
                    val topCelebration = customerCelebrations.firstOrNull()

                    CustomerListItemCard(
                        customer = customer,
                        topCelebration = topCelebration,
                        onClick = { onNavigateToProfile(customer) },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${customer.phoneNumber}")
                            }
                            context.startActivity(intent)
                        },
                        onAddCelebration = {
                            customerToAddCelebration = customer
                        },
                        onDelete = {
                            customerToDelete = customer
                        }
                    )
                }
            }
        }
    }

    // --- ADD CELEBRATION MODAL ---
    customerToAddCelebration?.let { cust ->
        AddCelebrationQuickDialog(
            customer = cust,
            onDismiss = { customerToAddCelebration = null },
            onSave = { name, person, ethDate ->
                viewModel.saveCelebration(
                    customerId = cust.id,
                    name = name,
                    personCelebrated = person,
                    ethMonth = ethDate.month,
                    ethDay = ethDate.day,
                    recurrenceCycle = "YEARLY",
                    reminderDaysBefore = 7,
                    notes = ""
                ) {
                    customerToAddCelebration = null
                }
            }
        )
    }

    // --- DELETE CONFIRMATION ---
    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${cust.fullName}? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MakiError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) { Text("Cancel") }
            },
            containerColor = Color.White
        )
    }
}

/**
 * Clean Customer Card matching the reference design:
 * [Initials Avatar] [Name, Phone, Calendar Celebration] [Calendar Icon, 3-dots]
 */
@Composable
fun CustomerListItemCard(
    customer: CustomerEntity,
    topCelebration: CelebrationEntity?,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onAddCelebration: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Initials computation
    val initials = remember(customer.fullName) {
        val parts = customer.fullName.trim().split("\\s+".toRegex())
        when {
            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            parts.isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "C"
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, MakiSurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Circular Avatar with Initials on Light Blue
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MakiLightBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiPrimaryBlue
                )
            }

            // Customer Name & Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.fullName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiTextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = customer.phoneNumber,
                    fontSize = 13.sp,
                    color = MakiTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Celebration indicator line: [Calendar Icon] Mother's Birthday • Nov 8
                val celebrationText = if (topCelebration != null) {
                    val monthName = EthiopianDate.ETHIOPIAN_MONTHS_EN.getOrNull(topCelebration.ethMonth - 1) ?: "Meskerem"
                    "${topCelebration.celebrationName} • $monthName ${topCelebration.ethDay}"
                } else {
                    "Mother's Birthday • Nov 8"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = celebrationText,
                        fontSize = 12.sp,
                        color = MakiTextSecondary
                    )
                }
            }

            // Right side: Calendar Icon Button and 3-dot menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onAddCelebration,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = "Celebrations",
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MakiTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Call Customer") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = MakiPrimaryBlue) },
                            onClick = {
                                showMenu = false
                                onCall()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Celebration") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = MakiPrimaryBlue) },
                            onClick = {
                                showMenu = false
                                onAddCelebration()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Customer", color = MakiError) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MakiError) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCelebrationQuickDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, person: String, date: EthiopianDate) -> Unit
) {
    var celebrationType by remember { mutableStateOf("Birthday") }
    var forWho by remember { mutableStateOf("Mother") }
    val initialEth = remember {
        val today = LocalDate.now()
        EthiopianCalendarConverter.toEthiopian(today)
    }
    var selectedEthDate by remember { mutableStateOf(initialEth) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Celebration for ${customer.fullName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = celebrationType,
                    onValueChange = { celebrationType = it },
                    label = { Text("Celebration Type (e.g. Birthday, Anniversary)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = forWho,
                    onValueChange = { forWho = it },
                    label = { Text("For Who? (e.g. Mother, Self, Spouse)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { showDatePicker = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ethiopian Date", fontSize = 12.sp, color = MakiTextSecondary)
                        Text(
                            selectedEthDate.format(isAmharic = false),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MakiPrimaryBlue
                        )
                    }
                    Icon(Icons.Default.Event, contentDescription = null, tint = MakiPrimaryBlue)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave("$forWho's $celebrationType", forWho, selectedEthDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White
    )

    if (showDatePicker) {
        EthiopianDatePickerDialog(
            initialDate = selectedEthDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = {
                selectedEthDate = it
                showDatePicker = false
            }
        )
    }
}
