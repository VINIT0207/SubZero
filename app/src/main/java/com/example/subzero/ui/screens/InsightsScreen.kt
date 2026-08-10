package com.example.subzero.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subzero.data.Asset
import com.example.subzero.data.AssetCategory
import com.example.subzero.viewmodel.AssetViewModel
import com.example.subzero.ui.theme.*
import java.util.*

@Composable
fun InsightsScreen(
    viewModel: AssetViewModel
) {
    val monthlyDrain by viewModel.monthlyDrain.collectAsState()
    val annualDrain by viewModel.annualDrain.collectAsState()
    val totalAssetsCount by viewModel.totalAssetsCount.collectAsState()
    val categoryBreakdowns by viewModel.categoryBreakdowns.collectAsState()
    val overlaps by viewModel.overlaps.collectAsState()
    val totalPotentialSavings by viewModel.totalPotentialSavings.collectAsState()
    val aiAdvice by viewModel.aiAdvice.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val currencySymbol = getCurrencySymbol(viewModel.currency.collectAsState().value)

    LaunchedEffect(Unit) {
        viewModel.fetchAiAdvisorAdvice()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    text = "SPENDING",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftGray,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("insights_hero_card"),
                    colors = CardDefaults.cardColors(containerColor = BaseSlateDarkDark),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "This month",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                 Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", monthlyDrain)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BaseDarkTextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Live burn rate",
                                    color = Color(0xFF81C784),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Projected annual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", annualDrain)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BaseDarkTextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "❄️ $totalAssetsCount active",
                                    color = SoftGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "SubZero Smart Advisor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2F33))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFA8C7FA),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Powered by Gemini AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA8C7FA),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAiLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFA8C7FA),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Text(
                                text = aiAdvice ?: "Analyzing your subscription patterns...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                lineHeight = 22.sp
                            )
                            
                            if (aiAdvice != null) {
                                Button(
                                    onClick = { viewModel.fetchAiAdvisorAdvice() },
                                    modifier = Modifier.padding(top = 16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2C2F33),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Refresh Insights", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Category breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            if (categoryBreakdowns.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No category data available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SoftGray
                        )
                    }
                }
            } else {
                items(categoryBreakdowns) { breakdown ->
                    CategoryBreakdownRow(
                        breakdown = breakdown,
                        currencySymbol = currencySymbol
                    )
                }
            }

            item {
                Text(
                    text = "Subscription fatigue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = MaterialTheme.shapes.large,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (overlaps.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1C2D24)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "No overlap detected",
                                        fontWeight = FontWeight.Bold,
                                        color = DarkText
                                    )
                                    Text(
                                        text = "SubZero didn't find any duplicate services across your bundle subscriptions.",
                                        fontSize = 12.sp,
                                        color = SoftGray,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        } else {
                            overlaps.forEach { overlap ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x33FFB4AB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("⚠️", fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Duplicate billing detected!",
                                            fontWeight = FontWeight.Bold,
                                            color = CoralAlert
                                        )
                                        Text(
                                            text = "You pay for standalone ${overlap.standaloneName} (${currencySymbol}${overlap.potentialSavings}/mo), but your ${overlap.bundleName} bundle already covers ${overlap.overlappingService}.",
                                            fontSize = 12.sp,
                                            color = DarkText,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = SageLight),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Potential Savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = SagePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalPotentialSavings)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SagePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estimated monthly savings if you cancel overlapping standalone services.",
                            fontSize = 13.sp,
                            color = SagePrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownRow(
    breakdown: AssetViewModel.CategoryBreakdown,
    currencySymbol: String
) {
    val barColor = when (breakdown.category) {
        AssetCategory.GYM -> SagePrimary
        AssetCategory.STREAMING -> CoralAlert
        AssetCategory.BUNDLE -> Color(0xFF4A90E2)
        AssetCategory.CURATED_BOX -> AmberWarning
        AssetCategory.SAAS -> SageSecondary
        else -> SoftGray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = breakdown.category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }
            Text(
                text = "$currencySymbol${String.format(Locale.US, "%,.2f", breakdown.amount)}/mo",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { breakdown.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = BorderGray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${String.format(Locale.US, "%.0f", breakdown.percentage * 100)}% of monthly burn",
            fontSize = 11.sp,
            color = SoftGray
        )
    }
}
