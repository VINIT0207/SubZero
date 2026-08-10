package com.example.subzero.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subzero.data.Asset
import com.example.subzero.data.AssetCategory
import com.example.subzero.viewmodel.AssetViewModel
import com.example.subzero.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAssetScreen(
    viewModel: AssetViewModel,
    assetId: Int, // 0 for adding
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val assets by viewModel.assets.collectAsState()
    val isEditMode = assetId != 0
    val existingAsset = remember(assetId, assets) { assets.find { it.id == assetId } }

    // Form states
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AssetCategory.SAAS) }
    var costStr by remember { mutableStateOf("") }
    var isVariable by remember { mutableStateOf(false) }
    var billingCycle by remember { mutableStateOf("Monthly") }
    var notes by remember { mutableStateOf("") }
    var serviceTag by remember { mutableStateOf("None") }
    
    // Date states
    var startDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var nextBillingDateMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var expiryDateMs by remember { mutableStateOf<Long?>(null) }

    // Trials states
    var isTrial by remember { mutableStateOf(false) }
    var trialLengthDaysStr by remember { mutableStateOf("14") }

    // Gift cards states
    var remainingBalanceStr by remember { mutableStateOf("") }

    // Category-specific states
    var location by remember { mutableStateOf("") }
    var gamingTag by remember { mutableStateOf("") }
    var deliveryFrequencyStr by remember { mutableStateOf("30") }
    var seatsStr by remember { mutableStateOf("") }
    
    // Bundle-nested services state
    val availableBundleServices = listOf("Music", "Video", "Cloud", "Storage", "TV", "Arcade", "Gaming")
    val selectedNestedServices = remember { mutableStateListOf<String>() }

    // Nudge days selections
    val nudgeOptions = listOf(1, 3, 7, 14, 30)
    val selectedNudges = remember { mutableStateListOf(1, 3, 7) }

    // Initialize editing values
    LaunchedEffect(existingAsset) {
        if (isEditMode && existingAsset != null) {
            name = existingAsset.name
            category = existingAsset.category
            costStr = existingAsset.cost.toString()
            isVariable = existingAsset.isVariable
            billingCycle = existingAsset.billingCycle
            notes = existingAsset.notes
            serviceTag = if (existingAsset.serviceTag.isEmpty()) "None" else existingAsset.serviceTag
            startDateMs = existingAsset.startDate
            nextBillingDateMs = existingAsset.nextBillingDate
            expiryDateMs = existingAsset.expiryDate
            isTrial = existingAsset.isTrial
            trialLengthDaysStr = existingAsset.trialLengthDays.toString()
            remainingBalanceStr = existingAsset.remainingBalance.toString()
            location = existingAsset.location
            gamingTag = existingAsset.gamingTag
            deliveryFrequencyStr = existingAsset.deliveryFrequencyDays.toString()
            seatsStr = existingAsset.seats.joinToString(", ")
            
            selectedNestedServices.clear()
            selectedNestedServices.addAll(existingAsset.nestedServices)

            selectedNudges.clear()
            selectedNudges.addAll(existingAsset.nudgesBeforeCharge)
        }
    }

    // Auto-calculating trials
    val calculatedTrialDeadlineStr = remember(startDateMs, trialLengthDaysStr) {
        val days = trialLengthDaysStr.toIntOrNull() ?: 0
        val deadlineMs = startDateMs + (days.toLong() * 24 * 60 * 60 * 1000)
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        sdf.format(Date(deadlineMs))
    }

    val dateFormatter = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditMode) "Edit Asset" else "New Asset", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. CATEGORY CHIPS SELECTOR
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SoftGray,
                letterSpacing = 1.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // We show horizontal scroll or wrapping chips for Category selection
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AssetCategory.values().toList().size) { index ->
                        val cat = AssetCategory.values()[index]
                        val isSelected = category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                category = cat
                                // Custom smart adaptations based on category
                                if (cat == AssetCategory.GIFT_CARD) {
                                    billingCycle = "One-time"
                                    isTrial = false
                                    costStr = "0.00"
                                } else if (cat == AssetCategory.GAMING_PASS) {
                                    serviceTag = "Gaming"
                                } else if (cat == AssetCategory.STREAMING) {
                                    serviceTag = "Video"
                                }
                            },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = SlateDark,
                                labelColor = SoftGray
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            // 2. NAME
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Netflix, Equinox Gym, Zara Gift Card") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("asset_name_input"),
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = BorderGray
                )
            )

            // 3. VALUE/COST OR GIFT CARD REMAINING BALANCE
            if (category == AssetCategory.GIFT_CARD) {
                OutlinedTextField(
                    value = remainingBalanceStr,
                    onValueChange = { remainingBalanceStr = it },
                    label = { Text("Remaining Balance ($)") },
                    placeholder = { Text("100.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gift_card_balance_input"),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = BorderGray
                    )
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cost ($)") },
                        placeholder = { Text("14.99") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("asset_cost_input"),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderGray
                        )
                    )

                    // Variable cost flag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Checkbox(
                            checked = isVariable,
                            onCheckedChange = { isVariable = it }
                        )
                        Text(
                            text = "Variable",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. BILLING CYCLE
            if (category != AssetCategory.GIFT_CARD) {
                Text(
                    text = "BILLING CYCLE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SoftGray,
                    letterSpacing = 1.sp
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Monthly", "Yearly", "Weekly", "Quarterly", "One-time").forEach { cycle ->
                        val isSel = billingCycle == cycle
                        FilterChip(
                            selected = isSel,
                            onClick = { billingCycle = cycle },
                            label = { Text(cycle, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SageSecondary,
                                selectedLabelColor = Color.White,
                                containerColor = SlateDark,
                                labelColor = SoftGray
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            // 5. SMART CATEGORY DYNAMIC FIELDS
            when (category) {
                AssetCategory.GYM -> {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Gym Location / Branch") },
                        placeholder = { Text("e.g. Soho Broadway, Downtown Club") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
                AssetCategory.SAAS, AssetCategory.STREAMING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = seatsStr,
                            onValueChange = { seatsStr = it },
                            label = { Text("Seats / Profiles") },
                            placeholder = { Text("e.g. Dad, Sis, Bob") },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )

                        // Service tags to detect overlaps (Subscription fatigue)
                        var expandedTags by remember { mutableStateOf(false) }
                        val tags = listOf("None", "Music", "Video", "Cloud", "Storage", "Gaming")
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = serviceTag,
                                onValueChange = {},
                                label = { Text("Service Tag") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { expandedTags = true }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedTags = true },
                                shape = MaterialTheme.shapes.medium
                            )
                            DropdownMenu(
                                expanded = expandedTags,
                                onDismissRequest = { expandedTags = false }
                            ) {
                                tags.forEach { tag ->
                                    DropdownMenuItem(
                                        text = { Text(tag) },
                                        onClick = {
                                            serviceTag = tag
                                            expandedTags = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                AssetCategory.GAMING_PASS -> {
                    OutlinedTextField(
                        value = gamingTag,
                        onValueChange = { gamingTag = it },
                        label = { Text("Linked Gamertag / Account") },
                        placeholder = { Text("e.g. PixelMaster7") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
                AssetCategory.CURATED_BOX -> {
                    OutlinedTextField(
                        value = deliveryFrequencyStr,
                        onValueChange = { deliveryFrequencyStr = it },
                        label = { Text("Delivery Frequency (Days)") },
                        placeholder = { Text("30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }
                AssetCategory.BUNDLE -> {
                    Text(
                        text = "INCLUDED NESTED SERVICES (for Overlap Fatigue Detection)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SoftGray
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableBundleServices.forEach { service ->
                            val isChecked = selectedNestedServices.contains(service)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) selectedNestedServices.remove(service)
                                    else selectedNestedServices.add(service)
                                },
                                label = { Text(service, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = SlateDark,
                                    labelColor = SoftGray
                                )
                            )
                        }
                    }
                }
                else -> {}
            }

            // 6. FREE TRIAL TOGGLE (non gift cards)
            if (category != AssetCategory.GIFT_CARD) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateLightCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("This is a Free Trial", fontWeight = FontWeight.Bold)
                                Text("Auto-calculate and warn before recurring charge", fontSize = 12.sp, color = SoftGray)
                            }
                            Switch(
                                checked = isTrial,
                                onCheckedChange = { isTrial = it }
                            )
                        }

                        if (isTrial) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = trialLengthDaysStr,
                                    onValueChange = { trialLengthDaysStr = it },
                                    label = { Text("Trial Length (Days)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                    singleLine = true
                                )
                                
                                // Start Date picker
                                val calendar = Calendar.getInstance().apply { timeInMillis = startDateMs }
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = dateFormatter.format(calendar.time),
                                        onValueChange = {},
                                        label = { Text("Start Date") },
                                        readOnly = true,
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                DatePickerDialog(
                                                    context,
                                                    { _, year, month, dayOfMonth ->
                                                        val cal = Calendar.getInstance().apply {
                                                            set(Calendar.YEAR, year)
                                                            set(Calendar.MONTH, month)
                                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                        }
                                                        startDateMs = cal.timeInMillis
                                                    },
                                                    calendar.get(Calendar.YEAR),
                                                    calendar.get(Calendar.MONTH),
                                                    calendar.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            }) {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "⏰ Cancellation Deadline: $calculatedTrialDeadlineStr\nAn alert will be scheduled 24 hours prior.",
                                fontSize = 13.sp,
                                color = CoralAlert,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // 7. DATE SELECTION
            Text(
                text = if (category == AssetCategory.GIFT_CARD) "VOID / EXPIRY DATE" else "NEXT BILLING DATE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SoftGray,
                letterSpacing = 1.sp
            )

            // Next Billing / Expiry Date Picker Card
            val dateToDisplay = if (category == AssetCategory.GIFT_CARD) {
                expiryDateMs ?: System.currentTimeMillis()
            } else {
                nextBillingDateMs
            }
            val displayCal = Calendar.getInstance().apply { timeInMillis = dateToDisplay }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                if (category == AssetCategory.GIFT_CARD) {
                                    expiryDateMs = cal.timeInMillis
                                } else {
                                    nextBillingDateMs = cal.timeInMillis
                                }
                            },
                            displayCal.get(Calendar.YEAR),
                            displayCal.get(Calendar.MONTH),
                            displayCal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = dateFormatter.format(displayCal.time),
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }
                    Text("Change", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 8. NUDGE WINDOWS BEFORE CHARGE
            Text(
                text = "ALARM NUDGES BEFORE CHARGE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SoftGray,
                letterSpacing = 1.sp
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                nudgeOptions.forEach { days ->
                    val isChecked = selectedNudges.contains(days)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            if (isChecked) selectedNudges.remove(days)
                            else selectedNudges.add(days)
                        },
                        label = { Text("${days}d before", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SagePrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = SlateDark,
                            labelColor = SoftGray
                        )
                    )
                }
            }

            // 9. NOTES
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Description") },
                placeholder = { Text("Any specific subscription codes, account tags, or cancellation notes...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = MaterialTheme.shapes.medium,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = BorderGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 10. SAVE ACTION BUTTON
            Button(
                onClick = {
                    if (name.trim().isEmpty()) return@Button

                    val finalCost = costStr.toDoubleOrNull() ?: 0.0
                    val finalBalance = remainingBalanceStr.toDoubleOrNull() ?: 0.0
                    val finalTrialLength = if (isTrial) trialLengthDaysStr.toIntOrNull() ?: 0 else 0

                    val calculatedNextBilling = if (isTrial) {
                        startDateMs + (finalTrialLength.toLong() * 24L * 60L * 60L * 1000L)
                    } else {
                        nextBillingDateMs
                    }

                    // Enforce alarm nudge for trials at 24 hours (1 day) prior
                    if (isTrial && !selectedNudges.contains(1)) {
                        selectedNudges.add(1)
                    }

                    val assetToSave = Asset(
                        id = if (isEditMode) assetId else 0,
                        name = name.trim(),
                        category = category,
                        cost = finalCost,
                        isVariable = isVariable,
                        billingCycle = billingCycle,
                        startDate = startDateMs,
                        nextBillingDate = calculatedNextBilling,
                        expiryDate = if (category == AssetCategory.GIFT_CARD) expiryDateMs else null,
                        isTrial = isTrial,
                        trialLengthDays = finalTrialLength,
                        remainingBalance = finalBalance,
                        notes = notes.trim(),
                        serviceTag = if (serviceTag == "None") "" else serviceTag,
                        location = location.trim(),
                        gamingTag = gamingTag.trim(),
                        deliveryFrequencyDays = deliveryFrequencyStr.toIntOrNull() ?: 30,
                        seats = seatsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        nestedServices = selectedNestedServices.toList(),
                        nudgesBeforeCharge = selectedNudges.toList(),
                        isCanceled = existingAsset?.isCanceled ?: false
                    )

                    if (isEditMode) {
                        viewModel.updateAsset(assetToSave)
                    } else {
                        viewModel.addAsset(assetToSave)
                    }
                    onBack()
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_asset_button"),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Add Asset",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
