package com.example.ui.celebrations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.calendar.EthiopianCalendarConverter
import com.example.domain.calendar.EthiopianDate
import com.example.localization.AppLanguage
import com.example.ui.theme.MakiError
import com.example.ui.theme.MakiLightBlue
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiSurfaceVariant
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.theme.MakiWarning
import com.example.ui.viewmodel.MakiViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CelebrationFilter {
    NEXT_7_DAYS,
    THIS_MONTH,
    ALL
}

@Composable
fun CelebrationsScreen(
    viewModel: MakiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAmharic = (currentLang == AppLanguage.AMHARIC)

    val celebrations by viewModel.celebrations.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf(CelebrationFilter.NEXT_7_DAYS) }

    val today = remember { LocalDate.now() }
    val custMap = remember(customers) { customers.associateBy { it.id } }

    val celebrationItems = remember(celebrations, custMap, allSales, filter) {
        celebrations.mapNotNull { cel ->
            val cust = custMap[cel.customerId]
            val nextDate = EthiopianCalendarConverter.getNextCelebrationDate(today, cel.ethMonth, cel.ethDay)
            val daysUntil = ChronoUnit.DAYS.between(today, nextDate)

            val matchesFilter = when (filter) {
                CelebrationFilter.NEXT_7_DAYS -> daysUntil in 0..7
                CelebrationFilter.THIS_MONTH -> daysUntil in 0..30
                CelebrationFilter.ALL -> true
            }

            if (matchesFilter) {
                val customerSales = allSales.filter { it.customerId == cel.customerId && !it.isVoided }
                val lastGift = customerSales.firstOrNull()?.productNameSnapshot ?: "Luxury Gift Set"
                val mName = if (isAmharic) EthiopianDate.ETHIOPIAN_MONTHS_AM[cel.ethMonth - 1] else EthiopianDate.ETHIOPIAN_MONTHS_EN[cel.ethMonth - 1]
                val ethDateStr = "$mName ${cel.ethDay}"

                Triple(cel, cust, Triple(daysUntil, lastGift, ethDateStr))
            } else null
        }.sortedBy { it.third.first }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cake, contentDescription = null, tint = MakiPrimaryBlue, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = strings.celebrations,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MakiPrimaryBlue
                )
                Text(
                    text = "Customer occasion timeline & re-engagement alerts",
                    fontSize = 13.sp,
                    color = MakiTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                CelebrationFilter.NEXT_7_DAYS to "Next 7 Days",
                CelebrationFilter.THIS_MONTH to "This Month",
                CelebrationFilter.ALL to "All Celebrations"
            ).forEach { (f, label) ->
                val isSelected = filter == f
                FilterChip(
                    selected = isSelected,
                    onClick = { filter = f },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MakiPrimaryBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (celebrationItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MakiSurfaceVariant)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MakiTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No occasions found for this filter.",
                        color = MakiTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(celebrationItems, key = { it.first.id }) { (cel, cust, details) ->
                    val (daysUntil, lastGift, ethDateStr) = details
                    val custName = cust?.fullName ?: "Customer"
                    val custPhone = cust?.phoneNumber ?: ""

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$custName — ${cel.celebrationName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MakiTextDark
                                    )
                                    Text(
                                        text = "Celebrated: ${cel.personCelebrated} (${cel.relationship}) • Date: $ethDateStr",
                                        fontSize = 13.sp,
                                        color = MakiTextSecondary
                                    )
                                    Text(
                                        text = "Last Purchased Gift: $lastGift",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MakiPrimaryBlue
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                daysUntil == 0L -> MakiError.copy(alpha = 0.15f)
                                                daysUntil <= 7 -> MakiWarning.copy(alpha = 0.18f)
                                                else -> MakiLightBlue
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            daysUntil == 0L -> strings.celebrateToday
                                            daysUntil == 1L -> "Tomorrow!"
                                            else -> "In $daysUntil Days"
                                        },
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = when {
                                            daysUntil == 0L -> MakiError
                                            daysUntil <= 7 -> Color(0xFFB45309)
                                            else -> MakiPrimaryBlue
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Contact shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (custPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:$custPhone")
                                            }
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.callCustomer, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val message = String.format(
                                                strings.marketingMessageTemplate,
                                                custName,
                                                cel.celebrationName,
                                                ethDateStr,
                                                lastGift
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=$custPhone&text=${Uri.encode(message)}")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                val share = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, message)
                                                }
                                                context.startActivity(Intent.createChooser(share, "Send Greeting"))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Text(strings.whatsappMessage, fontSize = 12.sp, color = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = { viewModel.markCelebrationNotified(cel.id) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Mark Contacted", tint = MakiSuccess)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
