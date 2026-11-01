package com.example.ui.more

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MakiLightBlue
import com.example.ui.theme.MakiPrimaryBlue
import com.example.ui.theme.MakiSurfaceBorder
import com.example.ui.theme.MakiTextDark
import com.example.ui.theme.MakiTextSecondary
import com.example.ui.viewmodel.MakiViewModel

@Composable
fun MoreScreen(
    viewModel: MakiViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCelebrations: () -> Unit = {},
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings by viewModel.strings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.more,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MakiPrimaryBlue
            )
            Text(
                text = "Business operations, history, analytics & management",
                fontSize = 13.sp,
                color = MakiTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            MoreNavigationItem(
                icon = Icons.Default.History,
                title = strings.salesHistory,
                subtitle = "View, search, filter and audit all past transactions",
                onClick = onNavigateToHistory,
                testTag = "more_sales_history"
            )
        }

        item {
            MoreNavigationItem(
                icon = Icons.Default.Cake,
                title = strings.celebrations,
                subtitle = "Upcoming customer birthdays, anniversaries & reminder alerts",
                onClick = onNavigateToCelebrations,
                testTag = "more_celebrations"
            )
        }

        item {
            MoreNavigationItem(
                icon = Icons.Default.Assessment,
                title = strings.reports,
                subtitle = "Revenue, gross profit, inventory valuation & top products",
                onClick = onNavigateToReports,
                testTag = "more_reports"
            )
        }

        item {
            MoreNavigationItem(
                icon = Icons.Default.Backup,
                title = strings.backupRestore,
                subtitle = "Export JSON backup and restore complete business database",
                onClick = onNavigateToBackup,
                testTag = "more_backup"
            )
        }

        item {
            MoreNavigationItem(
                icon = Icons.Default.Settings,
                title = strings.settings,
                subtitle = "Language (English/Amharic), currency, calendar & about",
                onClick = onNavigateToSettings,
                testTag = "more_settings"
            )
        }
    }
}

@Composable
fun MoreNavigationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MakiLightBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MakiPrimaryBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MakiTextDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MakiTextSecondary
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MakiTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
