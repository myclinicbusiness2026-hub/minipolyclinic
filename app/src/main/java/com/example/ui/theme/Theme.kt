package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MedicalGreen,
    onPrimary = PureWhite,
    primaryContainer = SoftGreenContainer,
    onPrimaryContainer = MedicalGreenDark,
    secondary = SkyBluePrimary,
    onSecondary = PureWhite,
    secondaryContainer = SkyBlueLight,
    onSecondaryContainer = SkyBlueDark,
    tertiary = MahoganyAccent,
    onTertiary = PureWhite,
    tertiaryContainer = MahoganyLight,
    onTertiaryContainer = MahoganyAccent,
    background = SoftLightGray,
    onBackground = DarkGrayText,
    surface = PureWhite,
    onSurface = DarkGrayText,
    surfaceVariant = SoftLightGray,
    onSurfaceVariant = SlateMedium,
    outline = BorderGray
)

@Composable
fun ShweKhitClinicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ShweKhitClinicTheme(darkTheme = darkTheme, content = content)
}

@Composable
fun clinicTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MedicalGreen,
    unfocusedBorderColor = BorderGray,
    focusedContainerColor = PureWhite,
    unfocusedContainerColor = PureWhite,
    focusedTextColor = DarkGrayText,
    unfocusedTextColor = DarkGrayText
)

@Composable
fun clinicSearchBarColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MedicalGreen,
    unfocusedBorderColor = BorderGray,
    focusedContainerColor = PureWhite,
    unfocusedContainerColor = PureWhite,
    focusedTextColor = DarkGrayText,
    unfocusedTextColor = DarkGrayText
)
