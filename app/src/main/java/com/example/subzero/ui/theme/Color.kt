package com.example.subzero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Raw/Static Colors for Theme Construction
val BaseCoolGreyLight = Color(0xFFF4F6F8)
val BaseCoolGreyDark = Color(0xFF131517)

val BaseSagePrimaryLight = Color(0xFF1A3F2C)
val BaseSagePrimaryDark = Color(0xFFD1E4FF)

val BaseSageSecondaryLight = Color(0xFF38654D)
val BaseSageSecondaryDark = Color(0xFF004977)

val BaseSageAccentLight = Color(0xFF517D63)
val BaseSageAccentDark = Color(0xFFD1E4FF)

val BaseSlateDarkLight = Color(0xFFFFFFFF)
val BaseSlateDarkDark = Color(0xFF1E2124)

val BaseSlateLightCardLight = Color(0xFFE9ECEF)
val BaseSlateLightCardDark = Color(0xFF2A2D31)

val BaseCoralAlertLight = Color(0xFFBA1A1A)
val BaseCoralAlertDark = Color(0xFFFFB4AB)

val BaseAmberWarningLight = Color(0xFFD27B00)
val BaseAmberWarningDark = Color(0xFFEBB15B)

val BaseUrgentCardBgLight = Color(0xFFFFDAD6)
val BaseUrgentCardBgDark = Color(0xFF410002)

val BaseUrgentCardBorderLight = Color(0xFFBA1A1A)
val BaseUrgentCardBorderDark = Color(0xFF93000A)

val BaseBorderGrayLight = Color(0xFFCED4DA)
val BaseBorderGrayDark = Color(0xFF343A40)

val BaseDarkTextLight = Color(0xFF1A1D20)
val BaseDarkTextDark = Color(0xFFE2E2E6)

// Base static definitions to avoid breakages in legacy imports/references
val CoolGreyLight = BaseCoolGreyLight
val CoolGreyDark = BaseCoolGreyDark
val UrgentCardBg = BaseUrgentCardBgDark
val UrgentCardBorder = BaseUrgentCardBorderDark
val SoftGray = Color(0xFF909194)

val SagePrimary: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.red < 0.5f) BaseSagePrimaryDark else BaseSagePrimaryLight

val SageLight: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.red < 0.5f) Color(0xFF1C221D) else Color(0xFFE8EFEA)

val SageSecondary = BaseSageSecondaryDark
val SageAccent = BaseSageAccentDark

// Theme-aware composable properties for seamless adaptive support:
val SlateDark: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val SlateLightCard: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant

val BorderGray: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline

val DarkText: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val CoralAlert: Color
    @Composable
    get() = MaterialTheme.colorScheme.error

val AmberWarning: Color
    @Composable
    get() = MaterialTheme.colorScheme.tertiary

val BlueInfo: Color
    @Composable
    get() = MaterialTheme.colorScheme.primaryContainer
