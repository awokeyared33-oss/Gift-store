package com.example.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.AppLanguage
import com.example.notifications.CelebrationReminderManager
import com.example.ui.theme.MakiLightBlue
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSuccess
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiSurfaceVariant
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.viewmodel.MakiViewModel

@Composable
fun SettingsScreen(
    viewModel: MakiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings by viewModel.strings.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MakiPrimaryBlue
            )
            Text(
                text = "Preferences, language, calendar & data ownership",
                fontSize = 13.sp,
                color = MakiTextSecondary
            )
        }

        // Language Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MakiPrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.languageLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MakiTextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = currentLang == AppLanguage.ENGLISH,
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            label = { Text("English") },
                            modifier = Modifier.testTag("lang_en_chip")
                        )
                        FilterChip(
                            selected = currentLang == AppLanguage.AMHARIC,
                            onClick = { viewModel.setLanguage(AppLanguage.AMHARIC) },
                            label = { Text("አማርኛ (Amharic)") },
                            modifier = Modifier.testTag("lang_am_chip")
                        )
                    }
                }
            }
        }

        // Currency & Calendar Preferences
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingInfoRow(
                        icon = Icons.Default.Payments,
                        title = strings.currencyLabel,
                        description = "Minor units precision calculation with BigDecimal"
                    )

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MakiSurfaceBorder))

                    SettingInfoRow(
                        icon = Icons.Default.CalendarMonth,
                        title = strings.calendarLabel,
                        description = "13 Months: 12 months x 30 days + Pagume (5/6 days)"
                    )
                }
            }
        }

        // Offline Data Privacy Notice
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MakiSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.localDataPrivacy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF065F46)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.privacyNotice,
                        fontSize = 13.sp,
                        color = Color(0xFF047857)
                    )
                }
            }
        }

        // Notifications Test
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MakiPrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Customer Celebration Alerts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Automated local notifications fire 7 days before customer birthdays & anniversaries.",
                        fontSize = 12.sp,
                        color = MakiTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            CelebrationReminderManager.checkAndNotifyCelebrations(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Check & Scan Upcoming Reminders Now")
                    }
                }
            }
        }

        // About MAKI BRANDS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MakiSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MakiPrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.aboutMakiBrands,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MakiTextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "MAKI BRANDS — Professional Offline Gift Shop Management & Customer Celebration CRM.\nEvery Sale. Every Customer. Every Celebration. Under Control.",
                        fontSize = 13.sp,
                        color = MakiTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.version,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MakiPrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun SettingInfoRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MakiLightBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MakiPrimaryBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MakiTextDark)
            Text(text = description, fontSize = 12.sp, color = MakiTextSecondary)
        }
    }
}
