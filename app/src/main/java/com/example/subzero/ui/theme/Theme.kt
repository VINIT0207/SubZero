package com.example.subzero.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BaseSageAccentDark,
    secondary = BaseSageSecondaryDark,
    tertiary = BaseAmberWarningDark,
    background = BaseCoolGreyDark,
    surface = BaseSlateDarkDark,
    onPrimary = Color(0xFF001D36),
    onSecondary = Color.White,
    onBackground = BaseDarkTextDark,
    onSurface = BaseDarkTextDark,
    surfaceVariant = BaseSlateLightCardDark,
    onSurfaceVariant = Color(0xFFC3C7D0),
    outline = BaseBorderGrayDark,
    error = BaseCoralAlertDark,
    errorContainer = BaseUrgentCardBgDark,
    onErrorContainer = BaseCoralAlertDark
)

private val LightColorScheme = lightColorScheme(
    primary = BaseSageAccentLight,
    secondary = BaseSageSecondaryLight,
    tertiary = BaseAmberWarningLight,
    background = BaseCoolGreyLight,
    surface = BaseSlateDarkLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = BaseDarkTextLight,
    onSurface = BaseDarkTextLight,
    surfaceVariant = BaseSlateLightCardLight,
    onSurfaceVariant = Color(0xFF43474E),
    outline = BaseBorderGrayLight,
    error = BaseCoralAlertLight,
    errorContainer = BaseUrgentCardBgLight,
    onErrorContainer = BaseCoralAlertLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
