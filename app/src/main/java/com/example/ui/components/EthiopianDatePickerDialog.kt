package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calendar.EthiopianCalendarConverter
import com.example.domain.calendar.EthiopianDate
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSurfaceBorder
import java.time.LocalDate

@Composable
fun EthiopianDatePickerDialog(
    initialDate: EthiopianDate? = null,
    isAmharic: Boolean = false,
    onDateSelected: (EthiopianDate) -> Unit,
    onDismissRequest: () -> Unit
) {
    val todayEth = remember { EthiopianCalendarConverter.toEthiopian(LocalDate.now()) }
    val base = initialDate ?: todayEth

    var currentYear by remember { mutableIntStateOf(base.year) }
    var currentMonth by remember { mutableIntStateOf(base.month) }
    var selectedDay by remember { mutableIntStateOf(base.day) }

    val maxDays = remember(currentYear, currentMonth) {
        if (currentMonth == 13) {
            if (EthiopianDate.isEthiopianLeapYear(currentYear)) 6 else 5
        } else 30
    }

    if (selectedDay > maxDays) {
        selectedDay = maxDays
    }

    val monthName = remember(currentMonth, isAmharic) {
        if (isAmharic) EthiopianDate.ETHIOPIAN_MONTHS_AM[currentMonth - 1]
        else EthiopianDate.ETHIOPIAN_MONTHS_EN[currentMonth - 1]
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Column {
                Text(
                    text = if (isAmharic) "የኢትዮጵያ ቀን መምረጫ" else "Ethiopian Calendar Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$monthName $selectedDay, $currentYear",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Year & Month Header Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonth > 1) {
                                currentMonth--
                            } else {
                                currentMonth = 13
                                currentYear--
                            }
                        },
                        modifier = Modifier.testTag("eth_prev_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { currentYear-- }) {
                                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "$currentYear",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { currentYear++ }) {
                                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentMonth < 13) {
                                currentMonth++
                            } else {
                                currentMonth = 1
                                currentYear++
                            }
                        },
                        modifier = Modifier.testTag("eth_next_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days Grid (up to 30, or 5/6 for Pagume)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(maxDays) { index ->
                        val day = index + 1
                        val isSelected = (day == selectedDay)
                        val isToday = (day == todayEth.day && currentMonth == todayEth.month && currentYear == todayEth.year)

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> MakiPrimaryBlue
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday && !isSelected) 1.5.dp else 1.dp,
                                    color = if (isToday && !isSelected) MakiPrimaryBlue else MakiSurfaceBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedDay = day }
                                .testTag("eth_day_$day"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$day",
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Today Button
                OutlinedButton(
                    onClick = {
                        currentYear = todayEth.year
                        currentMonth = todayEth.month
                        selectedDay = todayEth.day
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eth_pick_today")
                ) {
                    Text(if (isAmharic) "ዛሬን ምረጥ" else "Select Today")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDateSelected(EthiopianDate(currentYear, currentMonth, selectedDay))
                },
                colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                modifier = Modifier.testTag("eth_confirm_date")
            ) {
                Text(if (isAmharic) "አረጋግጥ" else "Select Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(if (isAmharic) "ተመለስ" else "Cancel")
            }
        }
    )
}
