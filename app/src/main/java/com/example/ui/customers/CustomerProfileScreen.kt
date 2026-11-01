package com.example.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CelebrationEntity
import com.example.data.local.entity.CustomerEntity
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

@Composable
fun CustomerProfileScreen(
    customer: CustomerEntity,
    viewModel: MakiViewModel,
    onNavigateBack: () -> Unit,
    onAddCelebration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isAmharic = (currentLang == AppLanguage.AMHARIC)

    val celebrations by viewModel.celebrations.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()

    val customerCelebrations = remember(celebrations, customer.id) {
        celebrations.filter { it.customerId == customer.id }
    }
    val customerSales = remember(allSales, customer.id) {
        allSales.filter { it.customerId == customer.id && !it.isVoided }
    }

    val today = remember { LocalDate.now() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("profile_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MakiPrimaryBlue)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Customer Profile",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MakiPrimaryBlue
                )
            }
        }

        // Customer Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MakiSurfaceBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MakiLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.fullName.take(1).uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MakiPrimaryBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = customer.fullName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MakiTextDark
                    )

                    Text(
                        text = "${customer.city} • Preferred Language: ${customer.preferredLanguage}",
                        fontSize = 13.sp,
                        color = MakiTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Phone: ${customer.phoneNumber}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MakiPrimaryBlue
                        )
                        if (customer.alternativePhone.isNotBlank()) {
                            Text(
                                text = " • Alt: ${customer.alternativePhone}",
                                fontSize = 14.sp,
                                color = MakiTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact Action Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${customer.phoneNumber}")
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("profile_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.callCustomer)
                        }

                        Button(
                            onClick = {
                                val lastGift = customerSales.firstOrNull()?.productNameSnapshot ?: "Special Gift"
                                val upcomingCel = customerCelebrations.firstOrNull()?.celebrationName ?: "upcoming celebration"
                                val msg = String.format(
                                    strings.marketingMessageTemplate,
                                    customer.fullName,
                                    upcomingCel,
                                    "upcoming special date",
                                    lastGift
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?phone=${customer.phoneNumber}&text=${Uri.encode(msg)}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Send WhatsApp"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("profile_whatsapp_button")
                        ) {
                            Text(strings.whatsappMessage, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Celebrations & Occasions Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = MakiPrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Celebrations & Occasions (${customerCelebrations.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MakiTextDark
                            )
                        }
                        TextButton(onClick = onAddCelebration) {
                            Text("+ Add Occasion", color = MakiPrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (customerCelebrations.isEmpty()) {
                        Text(
                            text = "No occasions recorded yet. Tap '+ Add Occasion' to register birthdays, anniversaries, and receive 7-day advance reminders.",
                            fontSize = 13.sp,
                            color = MakiTextSecondary
                        )
                    } else {
                        customerCelebrations.forEach { cel ->
                            val nextDate = EthiopianCalendarConverter.getNextCelebrationDate(today, cel.ethMonth, cel.ethDay)
                            val daysUntil = ChronoUnit.DAYS.between(today, nextDate)
                            val mName = if (isAmharic) EthiopianDate.ETHIOPIAN_MONTHS_AM[cel.ethMonth - 1] else EthiopianDate.ETHIOPIAN_MONTHS_EN[cel.ethMonth - 1]
                            val dateStr = "$mName ${cel.ethDay}"

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MakiSurfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${cel.celebrationName} (${cel.personCelebrated})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MakiTextDark
                                        )
                                        Text(
                                            text = "Date: $dateStr (Ethiopian) • ${cel.recurrenceCycle}",
                                            fontSize = 12.sp,
                                            color = MakiTextSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (daysUntil <= 7) MakiError.copy(alpha = 0.15f) else MakiPrimaryBlue.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (daysUntil == 0L) strings.celebrateToday else "$daysUntil Days Left",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (daysUntil <= 7) MakiError else MakiPrimaryBlue
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Customer Gift Preferences Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MakiPrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gift Preferences & Tastes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MakiTextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MakiLightBlue)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (customer.notes.isNotBlank()) customer.notes else "Prefers custom luxury packages, dark chocolates, floral fragrances with velvet ribbons.",
                            fontSize = 13.sp,
                            color = MakiPrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Verified Previous Purchases History (Distinct from celebration date!)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MakiPrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Purchase History (${customerSales.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MakiTextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (customerSales.isEmpty()) {
                        Text(
                            text = "No verified purchases recorded for this customer yet.",
                            fontSize = 13.sp,
                            color = MakiTextSecondary
                        )
                    } else {
                        customerSales.forEach { sale ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${sale.quantity}x ${sale.productNameSnapshot}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Purchased on: ${sale.ethiopianDateStr}",
                                        fontSize = 12.sp,
                                        color = MakiTextSecondary
                                    )
                                }
                                Text(
                                    text = "${sale.totalRevenue} ETB",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MakiPrimaryBlue
                                )
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MakiSurfaceBorder))
                        }
                    }
                }
            }
        }
    }
}
