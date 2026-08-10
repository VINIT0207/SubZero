package com.example.subzero.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AssetViewModel,
    onAddAsset: () -> Unit,
    onEditAsset: (Int) -> Unit,
    onNavigateToAlerts: () -> Unit
) {
    val assets by viewModel.assets.collectAsState()
    val monthlyDrain by viewModel.monthlyDrain.collectAsState()
    val annualDrain by viewModel.annualDrain.collectAsState()
    val activeTrialsCount by viewModel.activeTrialsCount.collectAsState()
    val totalAssetsCount by viewModel.totalAssetsCount.collectAsState()
    val currencySymbol = getCurrencySymbol(viewModel.currency.collectAsState().value)

    var selectedFilter by remember { mutableStateOf("All") }
    var expandedAssetId by remember { mutableStateOf<Int?>(null) }

    // Dynamic greeting based on current local time
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Filter categories list
    val filters = listOf("All", "Urgent", "Trials", "SaaS", "Streaming", "Gaming Pass", "Gift Card", "Gym", "Bundle", "Curated Box")

    // Filtered assets list
    val filteredAssets = remember(assets, selectedFilter) {
        val now = System.currentTimeMillis()
        val list = assets.sortedWith(compareBy<Asset> { it.isCanceled }
            .thenBy { if (it.category == AssetCategory.GIFT_CARD) it.expiryDate ?: Long.MAX_VALUE else it.nextBillingDate })

        when (selectedFilter) {
            "All" -> list
            "Urgent" -> list.filter { 
                !it.isCanceled && 
                (if (it.category == AssetCategory.GIFT_CARD) it.expiryDate ?: 0L else it.nextBillingDate) - now < TimeUnit.DAYS.toMillis(7) 
            }
            "Trials" -> list.filter { it.isTrial }
            else -> list.filter { it.category.displayName.equals(selectedFilter, ignoreCase = true) }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAsset,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_asset_fab")
                    .padding(bottom = 16.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add subscription/asset")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftGray
                    )
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = onNavigateToAlerts,
                    modifier = Modifier
                        .background(SlateDark, shape = CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = MaterialTheme.colorScheme.primary
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

            // Burn Rate Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("burn_rate_header"),
                colors = CardDefaults.cardColors(containerColor = BaseSlateDarkDark),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MONTHLY DRAIN",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftGray,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF202C24), shape = CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF5D7A68))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$totalAssetsCount active",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", monthlyDrain)}",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BaseDarkTextDark
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Annual", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", annualDrain)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BaseDarkTextDark
                            )
                        }
                        Column {
                            Text(text = "Trials", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                            Text(
                                text = "$activeTrialsCount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BaseDarkTextDark
                            )
                        }
                        Column {
                            Text(text = "Assets", style = MaterialTheme.typography.labelSmall, color = SoftGray)
                            Text(
                                text = "${assets.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BaseDarkTextDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Horizontal Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(text = filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = SlateDark,
                            labelColor = SoftGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderGray,
                            selectedBorderColor = Color.Transparent
                        ),
                        shape = CircleShape
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upcoming Title Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Charges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${filteredAssets.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGray
                )
            }

            // Assets list or Empty state
            if (filteredAssets.isEmpty()) {
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
                            text = "❄️",
                            fontSize = 48.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Zero financial leakage detected!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Add your recurring subscriptions, active free trials, or store credit gift cards to start freezing unneeded costs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredAssets, key = { it.id }) { asset ->
                        AssetItemCard(
                            asset = asset,
                            currencySymbol = currencySymbol,
                            isExpanded = expandedAssetId == asset.id,
                            onToggleExpand = {
                                expandedAssetId = if (expandedAssetId == asset.id) null else asset.id
                            },
                            onRenew = {
                                viewModel.markAsRenewed(asset)
                                expandedAssetId = null
                            },
                            onToggleCancel = {
                                viewModel.toggleCanceledStatus(asset)
                                expandedAssetId = null
                            },
                            onEdit = { onEditAsset(asset.id) },
                            onDelete = {
                                viewModel.deleteAsset(asset)
                                expandedAssetId = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetItemCard(
    asset: Asset,
    currencySymbol: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRenew: () -> Unit,
    onToggleCancel: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val targetTime = if (asset.category == AssetCategory.GIFT_CARD) {
        asset.expiryDate ?: now
    } else {
        asset.nextBillingDate
    }

    val daysRemaining = remember(targetTime) {
        val diffMs = targetTime - now
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        if (days < 0) 0 else days
    }

    val isUrgent = daysRemaining <= 3 && !asset.isCanceled

    val urgencyText = when {
        asset.isCanceled -> "Canceled"
        daysRemaining == 0L -> "Today"
        daysRemaining == 1L -> "Tomorrow"
        else -> "In $daysRemaining days"
    }

    val urgencyColor = when {
        asset.isCanceled -> SoftGray
        daysRemaining <= 1L -> CoralAlert
        daysRemaining <= 5L -> AmberWarning
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
            .clickable { onToggleExpand() }
            .animateContentSize()
            .testTag("asset_card_${asset.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular initials avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
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

                // Detail information
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (asset.isCanceled) SoftGray else DarkText,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (asset.isTrial && !asset.isCanceled) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFDF2E9), shape = MaterialTheme.shapes.small)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TRIAL",
                                    color = Color(0xFFD35400),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (asset.isCanceled) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFCE4EC), shape = MaterialTheme.shapes.small)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Canceled",
                                    color = CoralAlert,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = asset.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGray
                        )
                        Text(text = "•", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                        Text(
                            text = urgencyText,
                            style = MaterialTheme.typography.bodySmall,
                            color = urgencyColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", style = MaterialTheme.typography.bodySmall, color = SoftGray)
                        Text(
                            text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(targetTime)),
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftGray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Asset Value / cost
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    if (asset.category == AssetCategory.GIFT_CARD) {
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%.2f", asset.remainingBalance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (asset.isCanceled) SoftGray else DarkText
                        )
                        Text(
                            text = "balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGray
                        )
                    } else {
                        val cycleDisplay = when (asset.billingCycle) {
                            "Monthly" -> "/mo"
                            "Yearly" -> "/yr"
                            "Weekly" -> "/wk"
                            "Quarterly" -> "/qt"
                            else -> ""
                        }
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%.2f", asset.cost)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (asset.isCanceled) SoftGray else DarkText
                        )
                        Text(
                            text = cycleDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGray
                        )
                    }
                }
            }

            // Quick actions shown when expanded
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateLightCard)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Extra Meta properties
                    if (asset.location.isNotEmpty()) {
                        Text(
                            text = "📍 Location: ${asset.location}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (asset.seats.isNotEmpty()) {
                        Text(
                            text = "👥 Active Profiles: ${asset.seats.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (asset.gamingTag.isNotEmpty()) {
                        Text(
                            text = "🎮 Gamertag: ${asset.gamingTag}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (asset.category == AssetCategory.BUNDLE && asset.nestedServices.isNotEmpty()) {
                        Text(
                            text = "📦 Nested Services: ${asset.nestedServices.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (asset.notes.isNotEmpty()) {
                        Text(
                            text = "📝 Notes: ${asset.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderGray)

                    // Actions Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left-side core asset status togglers
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (asset.category != AssetCategory.GIFT_CARD && !asset.isCanceled) {
                                Button(
                                    onClick = onRenew,
                                    colors = ButtonDefaults.buttonColors(containerColor = SageSecondary, contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Renewed", fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = onToggleCancel,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (asset.isCanceled) SagePrimary else Color(0xFFFADBD8),
                                    contentColor = if (asset.isCanceled) Color.White else Color(0xFF900C3F)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = if (asset.isCanceled) Icons.Default.Restore else Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (asset.isCanceled) "Restore" else "Cancel", fontSize = 12.sp)
                            }
                        }

                        // Right-side editing actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SlateLightCard, shape = CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = SagePrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0x33FFB4AB), shape = CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralAlert, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Global Currency Helper
fun getCurrencySymbol(currency: String): String {
    return when (currency) {
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        "INR" -> "₹"
        "CAD" -> "CA$"
        "AUD" -> "A$"
        "SGD" -> "S$"
        "CHF" -> "Fr"
        "CNY" -> "¥"
        else -> "$"
    }
}
