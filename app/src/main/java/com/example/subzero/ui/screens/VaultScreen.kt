package com.example.subzero.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.os.Build
import android.app.AlarmManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subzero.viewmodel.AssetViewModel
import com.example.subzero.ui.theme.*
import com.example.subzero.utils.EncryptionHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: AssetViewModel
) {
    val context = LocalContext.current
    val currency by viewModel.currency.collectAsState()
    val isBiometricRequired by viewModel.isBiometricRequired.collectAsState()
    val isAutoLockEnabled by viewModel.isAutoLockEnabled.collectAsState()
    val isLocalNotificationsEnabled by viewModel.isLocalNotificationsEnabled.collectAsState()
    val defaultNudges by viewModel.defaultNudgeWindows.collectAsState()
    val notificationHour by viewModel.notificationHour.collectAsState()
    val notificationMinute by viewModel.notificationMinute.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportType by remember { mutableStateOf("JSON") }
    var exportString by remember { mutableStateOf("") }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var showCsvExportPasswordDialog by remember { mutableStateOf(false) }
    var csvExportPassword by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showNudgesDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var hasPostNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.canScheduleExactAlarms() ?: true
            } else {
                true
            }
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPostNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    alarmManager?.canScheduleExactAlarms() ?: true
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECURITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftGray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Vault",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("vault_hero_card"),
            colors = CardDefaults.cardColors(containerColor = BaseSlateDarkDark),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BaseSlateDarkDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Vault is locked & local",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Fully secure on this device. Your data never leaves your phone.",
                            fontSize = 13.sp,
                            color = Color(0xFFC3C7D0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Bank-Grade", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Security Level", fontSize = 11.sp, color = Color(0xFFC3C7D0))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "0", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Cloud uploads", fontSize = 11.sp, color = Color(0xFFC3C7D0))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "You", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "Sole owner", fontSize = 11.sp, color = Color(0xFFC3C7D0))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SECURITY SETTINGS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        PreferenceSwitchRow(
            title = "Biometric unlock",
            subtitle = "Use your Fingerprint or Face ID to unlock your subscription vault.",
            icon = Icons.Default.Fingerprint,
            checked = isBiometricRequired,
            onCheckedChange = { viewModel.setBiometricRequired(it) }
        )

        PreferenceSwitchRow(
            title = "Auto-lock on background",
            subtitle = "Automatically lock the app immediately when you leave or switch screens.",
            icon = Icons.Default.Lock,
            checked = isAutoLockEnabled,
            onCheckedChange = { viewModel.setAutoLockEnabled(it) }
        )

        PreferenceSwitchRow(
            title = "Local notifications",
            subtitle = "Receive friendly payment reminders directly on your phone's screen.",
            icon = Icons.Default.Notifications,
            checked = isLocalNotificationsEnabled,
            onCheckedChange = { viewModel.setLocalNotificationsEnabled(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "BACKUP & RECOVERY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        PreferenceActionRow(
            title = "Export secure backup",
            subtitle = "Save a highly secure, private copy of your subscriptions to your device.",
            icon = Icons.Default.IosShare,
            onClick = {
                exportType = "JSON"
                exportPassword = ""
                showExportPasswordDialog = true
            }
        )

        PreferenceActionRow(
            title = "Export spreadsheet (CSV)",
            subtitle = "Get a clean list of your active subscriptions that you can open in Excel.",
            icon = Icons.Default.Description,
            onClick = {
                exportType = "CSV"
                csvExportPassword = ""
                showCsvExportPasswordDialog = true
            }
        )

        PreferenceActionRow(
            title = "Restore backup list",
            subtitle = "Load your saved subscription list back into SubZero from a previous backup.",
            icon = Icons.Default.SettingsBackupRestore,
            onClick = { showImportDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GLOBAL PREFERENCES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        PreferenceActionRow(
            title = "Currency",
            subtitle = "Currently: $currency (${getCurrencySymbol(currency)})",
            icon = Icons.Default.Paid,
            onClick = { showCurrencyDialog = true }
        )

        val themeDisplay = when (themeMode) {
            "LIGHT" -> "Elegant Light"
            "DARK" -> "Elegant Dark"
            else -> "Follow System"
        }

        PreferenceActionRow(
            title = "Theme",
            subtitle = "Currently: $themeDisplay",
            icon = Icons.Default.Palette,
            onClick = { showThemeDialog = true },
            modifier = Modifier.testTag("pref_theme")
        )

        PreferenceActionRow(
            title = "Default nudge windows",
            subtitle = "Remind me: ${defaultNudges.joinToString(", ")} days before a charge.",
            icon = Icons.Default.Alarm,
            onClick = { showNudgesDialog = true }
        )

        val displayTime = remember(notificationHour, notificationMinute) {
            val amPm = if (notificationHour >= 12) "PM" else "AM"
            val displayHour = when {
                notificationHour == 0 -> 12
                notificationHour > 12 -> notificationHour - 12
                else -> notificationHour
            }
            String.format(Locale.US, "%d:%02d %s", displayHour, notificationMinute, amPm)
        }

        PreferenceActionRow(
            title = "Notification time",
            subtitle = "Alerts will arrive exactly at: $displayTime",
            icon = Icons.Default.AccessTime,
            onClick = {
                android.app.TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        viewModel.setNotificationTime(selectedHour, selectedMinute)
                    },
                    notificationHour,
                    notificationMinute,
                    false
                ).show()
            },
            modifier = Modifier.testTag("pref_notification_time")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PERMISSIONS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        PreferenceActionRow(
            title = "Notification Permission",
            subtitle = if (hasPostNotificationPermission) "Status: GRANTED" else "Status: DENIED (Tap to request)",
            icon = Icons.Default.Notifications,
            onClick = {
                if (!hasPostNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        androidx.core.app.ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                            101
                        )
                    }
                } else {
                    Toast.makeText(context, "Notifications are fully permitted!", Toast.LENGTH_SHORT).show()
                }
            }
        )

        PreferenceActionRow(
            title = "Exact Alarms Permission",
            subtitle = if (hasExactAlarmPermission) "Status: GRANTED" else "Status: DENIED (Tap to grant in Settings)",
            icon = Icons.Default.Timer,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open exact alarm settings.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Exact alarms are permitted!", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ABOUT & PRIVACY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = SoftGray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        PreferenceActionRow(
            title = "What SubZero does",
            subtitle = "Learn how we protect your personal details with absolute offline privacy.",
            icon = Icons.Default.HelpOutline,
            onClick = { showAboutDialog = true }
        )

        PreferenceActionRow(
            title = "Privacy Policy",
            subtitle = "No trackers. No analytics. We literally can never see your personal data.",
            icon = Icons.Default.Shield,
            onClick = { showPrivacyDialog = true }
        )

        PreferenceActionRow(
            title = "Contact Support",
            subtitle = "support.subzero.app@gmail.com",
            icon = Icons.Default.Email,
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:support.subzero.app@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "SubZero Feedback")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Email client not found.", Toast.LENGTH_LONG).show()
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SubZero • v1.0 • Cryptographic Local Vault",
                fontSize = 11.sp,
                color = SoftGray
            )
        }
    }

    if (showExportPasswordDialog) {
        var passwordVisibility by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("Backup Password Protection") },
            text = {
                Column {
                    Text(
                        text = "Set an optional password to encrypt your secure backup. If left blank, SubZero will encrypt your data with a standard cryptographic key. Remember this password, as you'll need it to restore this backup.",
                        fontSize = 13.sp,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Backup Password (Optional)") },
                        placeholder = { Text("Enter encryption password") },
                        visualTransformation = if (passwordVisibility) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                Icon(
                                    imageVector = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    exportString = viewModel.exportBackupJson(exportPassword)
                    showExportPasswordDialog = false
                    showExportDialog = true
                }) {
                    Text("Generate Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCsvExportPasswordDialog) {
        var passwordVisibility by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCsvExportPasswordDialog = false },
            title = { Text("Export Spreadsheet (CSV)") },
            text = {
                Column {
                    Text(
                        text = "Set an optional password to encrypt your spreadsheet file. If left blank, SubZero will download a clean, raw CSV file. If encrypted, the file will contain password-secured cryptographic payload.",
                        fontSize = 13.sp,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = csvExportPassword,
                        onValueChange = { csvExportPassword = it },
                        label = { Text("CSV Encryption Password (Optional)") },
                        placeholder = { Text("Enter encryption password") },
                        visualTransformation = if (passwordVisibility) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                Icon(
                                    imageVector = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val csvRaw = viewModel.exportBackupCsv()
                    val isEncrypted = csvExportPassword.isNotEmpty()
                    val finalFileName = if (isEncrypted) "subzero_subscriptions_encrypted.csv" else "subzero_subscriptions.csv"
                    val finalContent = if (isEncrypted) {
                        EncryptionHelper.encrypt(csvRaw, csvExportPassword)
                    } else {
                        csvRaw
                    }

                    val uri = EncryptionHelper.saveFileToDownloads(context, finalFileName, finalContent)
                    if (uri != null) {
                        Toast.makeText(context, "Spreadsheet downloaded!", Toast.LENGTH_LONG).show()
                    } else {
                        val clip = ClipData.newPlainText("SubZero CSV Backup", finalContent)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Failed to write file. Data copied to clipboard!", Toast.LENGTH_LONG).show()
                    }

                    exportString = finalContent
                    showCsvExportPasswordDialog = false
                    showExportDialog = true
                }) {
                    Text("Download & Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvExportPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportDialog) {
        val descriptionText = if (exportType == "CSV") {
            "Spreadsheet has been saved to your Downloads folder! You can also copy the data below or click Share:"
        } else {
            "Backup generated! Copy the payload below or click Share to transmit via Bluetooth, USB, or local file save:"
        }
        val copyButtonText = if (exportType == "CSV") "Copy data" else "Copy payload"

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Vault ($exportType)") },
            text = {
                Column {
                    Text(
                        text = descriptionText,
                        fontSize = 13.sp,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = exportString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, exportString)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Backup Vault")
                        context.startActivity(shareIntent)
                        showExportDialog = false
                    }) {
                        Text("Share")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val clip = ClipData.newPlainText("SubZero Backup", exportString)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup copied!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }) {
                        Text(copyButtonText)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showImportDialog) {
        var importPasswordVisibility by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore Vault") },
            text = {
                Column {
                    Text(
                        text = "Paste your SubZero backup JSON payload below to reconstruct your assets locally:",
                        fontSize = 13.sp,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("Paste your backup string here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Encryption password (if any):",
                        fontSize = 12.sp,
                        color = DarkText,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        placeholder = { Text("Enter backup password") },
                        visualTransformation = if (importPasswordVisibility) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { importPasswordVisibility = !importPasswordVisibility }) {
                                Icon(
                                    imageVector = if (importPasswordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importText.trim().isEmpty()) return@Button
                    val success = viewModel.importBackupJson(importText, importPassword)
                    if (success) {
                        Toast.makeText(context, "Vault restored!", Toast.LENGTH_SHORT).show()
                        importPassword = ""
                        showImportDialog = false
                    } else {
                        Toast.makeText(context, "Decryption or restore failed!", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("Restore Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCurrencyDialog) {
        val currencies = listOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "SGD", "CHF", "CNY")
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Default Currency") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { curr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrency(curr)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "$curr (${getCurrencySymbol(curr)})", fontWeight = FontWeight.Bold)
                            if (currency == curr) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showThemeDialog) {
        val themes = listOf(
            "SYSTEM" to "Follow System",
            "DARK" to "Elegant Dark",
            "LIGHT" to "Elegant Light"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Visual Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themes.forEach { (mode, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = name, fontWeight = FontWeight.Bold)
                            if (themeMode == mode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showNudgesDialog) {
        val nudgeOptions = listOf(1, 3, 7, 14, 30)
        val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(defaultNudges) } }
        AlertDialog(
            onDismissRequest = { showNudgesDialog = false },
            title = { Text("Default Alarm Nudges") },
            text = {
                Column {
                    Text("Select default nudge offsets scheduled for newly added active subscriptions:", fontSize = 13.sp, color = SoftGray, modifier = Modifier.padding(bottom = 12.dp))
                    nudgeOptions.forEach { days ->
                        val isChecked = selectedDays.contains(days)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedDays.remove(days)
                                    else selectedDays.add(days)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selectedDays.add(days)
                                    else selectedDays.remove(days)
                                }
                            )
                            Text(text = "$days day(s) before renewal", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setDefaultNudgeWindows(selectedDays.toList())
                    showNudgesDialog = false
                }) {
                    Text("Save preferences")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNudgesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = SagePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("About SubZero")
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "SubZero is an ultra-secure, completely offline subscription and financial asset vault.",
                        style = MaterialTheme.typography.titleSmall,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "• Zero Cloud, Zero Leak: Your pricing, notes, passwords, and billing information are fully encrypted using industry-standard cryptography locally on your physical device.\n\n" +
                               "• Intelligent Reminders: A fully offline notification scheduler that queues exact Android alerts, ensuring you cancel free trials and check upcoming billing on time.\n\n" +
                               "• Pure Utilities: Track remaining gift card balances, view monthly renewal projections, and catalog subscription details with advanced tags.",
                        fontSize = 13.sp,
                        color = SoftGray,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SagePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Privacy Policy")
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "100% Privacy by Design. No exceptions.",
                        style = MaterialTheme.typography.titleSmall,
                        color = DarkText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "• Zero Data Transmission: All subscription metadata and logs reside in an offline encrypted Room SQLite database.\n\n" +
                               "• Biometric Lock & Encryption: Keys are securely stored inside Android's Hardware Keystore system. If you delete the application, your local data is purged permanently.\n\n" +
                               "• No Trackers & Ad Networks: We have disabled all internet analytics. Zero third-party trackers are integrated into SubZero.",
                        fontSize = 13.sp,
                        color = SoftGray,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Accept & Close")
                }
            }
        )
    }
}

@Composable
fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(SlateDark, shape = MaterialTheme.shapes.large)
            .border(width = 1.dp, color = BorderGray, shape = MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SlateLightCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = SagePrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = DarkText)
                Text(text = subtitle, fontSize = 12.sp, color = SoftGray, lineHeight = 16.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PreferenceActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .background(SlateDark, shape = MaterialTheme.shapes.large)
            .border(width = 1.dp, color = BorderGray, shape = MaterialTheme.shapes.large)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SlateLightCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = SagePrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = DarkText)
                Text(text = subtitle, fontSize = 12.sp, color = SoftGray, lineHeight = 16.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SoftGray)
    }
}
