package com.example.subzero.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subzero.data.Asset
import com.example.subzero.data.AssetCategory
import com.example.subzero.viewmodel.AssetViewModel
import com.example.subzero.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class NudgeItem(
    val asset: Asset,
    val nudgeDays: Int,
    val nudgeTimeMs: Long,
    val targetTimeMs: Long,
    val daysUntilCharge: Long
)

@Composable
fun AlertsScreen(
    viewModel: AssetViewModel
) {
    val assets by viewModel.assets.collectAsState()
    val currencySymbol = getCurrencySymbol(viewModel.currency.collectAsState().value)
    val now = remember { System.currentTimeMillis() }

    // Compile active, scheduled nudges
    val allNudges = remember(assets, now) {
        val list = mutableListOf<NudgeItem>()
        val activeAssets = assets.filter { !it.isCanceled }

        for (asset in activeAssets) {
            val targetTime = if (asset.category == AssetCategory.GIFT_CARD) {
                asset.expiryDate
            } else {
                asset.nextBillingDate
            } ?: continue

            val daysUntil = TimeUnit.MILLISECONDS.toDays(targetTime - now).coerceAtLeast(0)

            for (nudgeDays in asset.nudgesBeforeCharge) {
                val nudgeTimeMs = targetTime - (nudgeDays.toLong() * 24L * 60L * 60L * 1000L)
                // If the nudge is upcoming
                if (nudgeTimeMs > now) {
                    list.add(
                        NudgeItem(
                            asset = asset,
                            nudgeDays = nudgeDays,
                            nudgeTimeMs = nudgeTimeMs,
                            targetTimeMs = targetTime,
                            daysUntilCharge = daysUntil
                        )
                    )
                }
            }
        }
        // Sort chronologically by nudge firing time
        list.sortBy { it.nudgeTimeMs }
        list.toList()
    }

    val partitionedNudges = remember(allNudges, now) {
        allNudges.partition { it.nudgeTimeMs - now <= TimeUnit.DAYS.toMillis(7) }
    }
    val thisWeekNudges = partitionedNudges.first
    val laterNudges = partitionedNudges.second

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NUDGES",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGray
                )
                Text(
                    text = "Alerts",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🔔 ${allNudges.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Exact Alarm warning banner if not granted on Android 12+
        var showAlarmWarning by remember { mutableStateOf(false) }
        val context = androidx.compose.ui.platform.LocalContext.current
        
        LaunchedEffect(Unit) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
                showAlarmWarning = alarmManager?.canScheduleExactAlarms() == false
            }
        }

        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
                        showAlarmWarning = alarmManager?.canScheduleExactAlarms() == false
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        if (showAlarmWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("exact_alarm_warning_banner"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏰ Precise Alarms Disabled",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SubZero needs 'Alarms & Reminders' permission to send precise nudges before trials expire.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                try {
                                    val intent = android.content.Intent().apply {
                                        action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (allNudges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "🔔",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Silence is golden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "No upcoming background alarms or nudges are scheduled. Your wallet is perfectly frozen for now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. THIS WEEK SECTION
                if (thisWeekNudges.isNotEmpty()) {
                    item {
                        Text(
                            text = "THIS WEEK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftGray,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
                        )
                    }
                    items(thisWeekNudges) { nudge ->
                        NudgeCard(
                            nudge = nudge,
                            currencySymbol = currencySymbol
                        )
                    }
                }

                // 2. LATER SECTION
                if (laterNudges.isNotEmpty()) {
                    item {
                        Text(
                            text = "LATER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftGray,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
                        )
                    }
                    items(laterNudges) { nudge ->
                        NudgeCard(
                            nudge = nudge,
                            currencySymbol = currencySymbol
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NudgeCard(
    nudge: NudgeItem,
    currencySymbol: String
) {
    val asset = nudge.asset
    val formatter = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }
    val dateStr = formatter.format(Date(nudge.nudgeTimeMs))

    val daysColor = when {
        nudge.daysUntilCharge <= 1 -> CoralAlert
        nudge.daysUntilCharge <= 5 -> AmberWarning
        else -> SageSecondary
    }

    val avatarBg = remember(asset.name) {
        val hash = asset.name.hashCode()
        val colors = listOf(
            Color(0xFFE3F2FD), Color(0xFFF1F8E9), Color(0xFFFFF3E0),
            Color(0xFFFCE4EC), Color(0xFFEDE7F6), Color(0xFFE0F2F1)
        )
        colors[Math.abs(hash) % colors.size]
    }

    val avatarText = remember(asset.name) {
        val words = asset.name.split(" ")
        if (words.size >= 2) {
            "${words[0].firstOrNull() ?: ' '}${words[1].firstOrNull() ?: ' '}".uppercase()
        } else {
            asset.name.take(2).uppercase()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("nudge_card_${asset.id}_${nudge.nudgeDays}"),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
        ) {
            // Main content column
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SagePrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    val valueText = if (asset.category == AssetCategory.GIFT_CARD) {
                        "Expires"
                    } else {
                        "Charges ${currencySymbol}${String.format(Locale.US, "%.2f", asset.cost)}"
                    }

                    val timePhrase = when (nudge.daysUntilCharge) {
                        0L -> "today"
                        1L -> "tomorrow"
                        else -> "in ${nudge.daysUntilCharge} days"
                    }

                    Text(
                        text = "$valueText $timePhrase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Nudge • ${nudge.nudgeDays}d before • $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right-hand urgency colored highlight block bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
                    .background(daysColor)
            )
        }
    }
}
