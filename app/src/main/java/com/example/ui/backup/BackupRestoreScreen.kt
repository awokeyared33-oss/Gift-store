package com.example.ui.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.repository.BackupPreview
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

@Composable
fun BackupRestoreScreen(
    viewModel: MakiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings by viewModel.strings.collectAsStateWithLifecycle()

    var isProcessing by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastExportedFileName by remember { mutableStateOf<String?>(null) }

    var backupContentToSave by remember { mutableStateOf<String?>(null) }
    var restorePreviewData by remember { mutableStateOf<BackupPreview?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    // SAF Document Creator for saving JSON backup file
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && backupContentToSave != null) {
            scope.launch {
                isProcessing = true
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(backupContentToSave!!.toByteArray(Charsets.UTF_8))
                        }
                    }
                    successMessage = strings.backupSuccess
                    isProcessing = false
                } catch (e: Exception) {
                    isProcessing = false
                    errorMessage = "Failed to write backup: ${e.localizedMessage}"
                }
            }
        }
    }

    // SAF Document Opener for restoring JSON backup file
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                        }
                    }
                    if (content != null) {
                        val preview = viewModel.parseBackupPreview(content)
                        pendingRestoreJson = content
                        restorePreviewData = preview
                    }
                    isProcessing = false
                } catch (e: Exception) {
                    isProcessing = false
                    errorMessage = "Failed to read backup file: ${e.localizedMessage}"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = strings.backupTitle,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MakiPrimaryBlue
        )
        Text(
            text = "Full offline JSON export & disaster restoration",
            fontSize = 13.sp,
            color = MakiTextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Cards
            if (isProcessing) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MakiLightBlue)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MakiPrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Processing data operation...", color = MakiPrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (successMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFECFDF5))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MakiSuccess)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(successMessage!!, color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MakiError)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(errorMessage!!, color = MakiError, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Export Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MakiLightBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = MakiPrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Business Data",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MakiTextDark
                                )
                                Text(
                                    text = "Includes all products, sales, customers, celebrations & prices",
                                    fontSize = 12.sp,
                                    color = MakiTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val defaultFilename = "MAKI_BRANDS_BACKUP_${LocalDate.now()}.json"

                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    successMessage = null
                                    errorMessage = null
                                    try {
                                        val json = viewModel.exportBackupJson()
                                        backupContentToSave = json
                                        lastExportedFileName = defaultFilename
                                        isProcessing = false
                                        // Launch SAF file chooser
                                        saveFileLauncher.launch(defaultFilename)
                                    } catch (e: Exception) {
                                        isProcessing = false
                                        errorMessage = "Failed to export: ${e.localizedMessage}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("backup_now_button")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.backupNow, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Share JSON backup option
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val json = viewModel.exportBackupJson()
                                        if (json.length > 500_000) {
                                            errorMessage = "Backup file is large (${json.length / 1024} KB with photos). Please use 'Backup Database Now' above to save the file directly."
                                        } else {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_SUBJECT, defaultFilename)
                                                putExtra(Intent.EXTRA_TEXT, json)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Backup JSON"))
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to share: ${e.localizedMessage}"
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Backup JSON via Messenger / Drive")
                        }
                    }
                }
            }

            // Restore Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MakiSurfaceBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = MakiWarning)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.restoreBackup,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MakiTextDark
                                )
                                Text(
                                    text = "Restore database from any previously exported JSON file",
                                    fontSize = 12.sp,
                                    color = MakiTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                successMessage = null
                                errorMessage = null
                                openFileLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_restore_file_button")
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Backup File (.json)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Restore Preview Dialog
    if (restorePreviewData != null && pendingRestoreJson != null) {
        val preview = restorePreviewData!!
        AlertDialog(
            onDismissRequest = {
                restorePreviewData = null
                pendingRestoreJson = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MakiWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.restorePreview, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.restoreWarning, color = MakiTextSecondary, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MakiSurfaceBorder))

                    Text("Schema Version: ${preview.schemaVersion}")
                    Text("${strings.productsToRestore}: ${preview.productCount}", fontWeight = FontWeight.SemiBold)
                    Text("${strings.salesToRestore}: ${preview.saleCount}", fontWeight = FontWeight.SemiBold)
                    Text("${strings.customersToRestore}: ${preview.customerCount}", fontWeight = FontWeight.SemiBold)
                    Text("${strings.celebrationsToRestore}: ${preview.celebrationCount}", fontWeight = FontWeight.SemiBold)
                    if (preview.movementCount > 0) {
                        Text("Inventory Movements: ${preview.movementCount}", fontWeight = FontWeight.SemiBold)
                    }
                    if (preview.expenseCount > 0) {
                        Text("Expenses: ${preview.expenseCount}", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingRestoreJson!!
                        restorePreviewData = null
                        pendingRestoreJson = null
                        isProcessing = true
                        viewModel.restoreBackup(json) { success, msg ->
                            isProcessing = false
                            if (success) {
                                successMessage = strings.restoreSuccess
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MakiPrimaryBlue),
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        restorePreviewData = null
                        pendingRestoreJson = null
                    }
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
