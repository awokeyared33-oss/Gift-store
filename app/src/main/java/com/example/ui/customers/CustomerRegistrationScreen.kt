package com.example.ui.customers

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calendar.EthiopianCalendarConverter
import com.example.domain.calendar.EthiopianDate
import com.example.ui.components.EthiopianDatePickerDialog
import com.example.ui.theme.MakiError
import com.example.ui.theme.MakiLightBlue
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.viewmodel.MakiViewModel
import java.time.LocalDate

@Composable
fun CustomerRegistrationScreen(
    viewModel: MakiViewModel,
    onNavigateBack: () -> Unit,
    onCustomerSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Customer Info Fields
    var fullName by remember { mutableStateOf("") }
    var primaryPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Celebration Details Fields
    val celebrationTypes = listOf("Birthday", "Anniversary", "Graduation", "Holiday", "Wedding", "Other")
    var selectedCelebrationType by remember { mutableStateOf("Birthday") }
    var showTypeDropdown by remember { mutableStateOf(false) }

    val forWhoOptions = listOf("Mother", "Father", "Spouse", "Child", "Friend", "Self", "Other")
    var selectedForWho by remember { mutableStateOf("Mother") }
    var showForWhoDropdown by remember { mutableStateOf(false) }

    val initialEth = remember {
        val today = LocalDate.now()
        EthiopianCalendarConverter.toEthiopian(today)
    }
    var selectedEthDate by remember { mutableStateOf(initialEth) }
    var showDatePicker by remember { mutableStateOf(false) }

    val repeatOptions = listOf("1 Year", "1 Month", "None")
    var selectedRepeat by remember { mutableStateOf("1 Year") }
    var showRepeatDropdown by remember { mutableStateOf(false) }

    var reminderActive by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val saveAction = {
        if (fullName.isBlank()) {
            errorMessage = "Full Name is required"
        } else if (primaryPhone.isBlank()) {
            errorMessage = "Phone Number is required"
        } else {
            errorMessage = null
            isSaving = true

            viewModel.saveCustomer(
                name = fullName.trim(),
                phone = primaryPhone.trim(),
                notes = notes.trim()
            ) { newCustId ->
                viewModel.saveCelebration(
                    customerId = newCustId,
                    name = "$selectedForWho's $selectedCelebrationType",
                    personCelebrated = selectedForWho,
                    ethMonth = selectedEthDate.month,
                    ethDay = selectedEthDate.day,
                    recurrenceCycle = if (selectedRepeat == "1 Year") "YEARLY" else if (selectedRepeat == "1 Month") "MONTHLY" else "NONE",
                    reminderDaysBefore = 7,
                    notes = "Registered via Add Customer form"
                ) {
                    isSaving = false
                    onCustomerSaved(newCustId)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. HEADER (Back Arrow + "Add Customer" + Floppy Disk Save Icon) ---
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("add_customer_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MakiTextDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Add Customer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MakiTextDark
                )

                IconButton(
                    onClick = { saveAction() },
                    modifier = Modifier.testTag("add_customer_save_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- 2. CIRCULAR PHOTO PLACEHOLDER ("Choose Photo (Optional)") ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MakiLightBlue)
                        .clickable { /* Photo Picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Choose Photo",
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Choose Photo (Optional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MakiTextSecondary
                )
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage!!,
                    color = MakiError,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // --- 3. PERSONAL INFORMATION TEXT FIELDS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Full Name *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MakiTextDark
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("Enter customer name", color = MakiTextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MakiPrimaryBlue,
                        unfocusedBorderColor = MakiSurfaceBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_name_input")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Phone Number *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MakiTextDark
                )
                OutlinedTextField(
                    value = primaryPhone,
                    onValueChange = { primaryPhone = it },
                    placeholder = { Text("09XXXXXXXX", color = MakiTextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MakiPrimaryBlue,
                        unfocusedBorderColor = MakiSurfaceBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_phone_input")
                )
            }
        }

        // --- 4. CELEBRATION DETAILS SECTION ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        tint = MakiPrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Celebration Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MakiTextDark
                    )
                }

                // Row 1: Celebration Type * | For Who? *
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Celebration Type
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Celebration Type *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MakiTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(10.dp))
                                    .clickable { showTypeDropdown = true }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedCelebrationType,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MakiTextDark
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MakiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showTypeDropdown,
                                onDismissRequest = { showTypeDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                celebrationTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedCelebrationType = type
                                            showTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // For Who?
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "For Who? *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MakiTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(10.dp))
                                    .clickable { showForWhoDropdown = true }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedForWho,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MakiTextDark
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MakiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showForWhoDropdown,
                                onDismissRequest = { showForWhoDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                forWhoOptions.forEach { who ->
                                    DropdownMenuItem(
                                        text = { Text(who) },
                                        onClick = {
                                            selectedForWho = who
                                            showForWhoDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 2: Date (Ethiopian Calendar) * | Repeat Every *
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ethiopian Date Picker
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Date (Ethiopian Calendar) *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MakiTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(10.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val monthName = EthiopianDate.ETHIOPIAN_MONTHS_EN.getOrNull(selectedEthDate.month - 1) ?: "Meskerem"
                            Text(
                                text = "$monthName ${selectedEthDate.day}, ${selectedEthDate.year}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MakiTextDark,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MakiTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Repeat Every
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Repeat Every *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MakiTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(10.dp))
                                    .clickable { showRepeatDropdown = true }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedRepeat,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MakiTextDark
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MakiTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showRepeatDropdown,
                                onDismissRequest = { showRepeatDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                repeatOptions.forEach { rep ->
                                    DropdownMenuItem(
                                        text = { Text(rep) },
                                        onClick = {
                                            selectedRepeat = rep
                                            showRepeatDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 3: Reminder Switch ("Reminder: 7 Days Before")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reminder: 7 Days Before",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MakiTextDark
                    )

                    Switch(
                        checked = reminderActive,
                        onCheckedChange = { reminderActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MakiPrimaryBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }
        }

        // --- 5. NOTES (OPTIONAL) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Notes (Optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MakiTextDark
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Any additional notes...", color = MakiTextSecondary) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MakiPrimaryBlue,
                        unfocusedBorderColor = MakiSurfaceBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_notes_input")
                )
            }
        }

        // --- 6. FULL-WIDTH BLUE BUTTON: [💾 Save Customer] ---
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { saveAction() },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_customer_save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Customer",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

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
