package dev.aether.manager.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Material 3 Expressive / Monet — Purple baseline (like theme.css)
private val md_primary_light = Color(0xFF6750A4)
private val md_onPrimary_light = Color(0xFFFFFFFF)
private val md_primaryContainer_light = Color(0xFFEADDFF)
private val md_onPrimaryContainer_light = Color(0xFF4F378B)
private val md_secondary_light = Color(0xFF625B71)
private val md_onSecondary_light = Color(0xFFFFFFFF)
private val md_secondaryContainer_light = Color(0xFFE8DEF8)
private val md_onSecondaryContainer_light = Color(0xFF4A4458)
private val md_tertiary_light = Color(0xFF7D5260)
private val md_onTertiary_light = Color(0xFFFFFFFF)
private val md_tertiaryContainer_light = Color(0xFFFFD8E4)
private val md_onTertiaryContainer_light = Color(0xFF633B48)
private val md_error_light = Color(0xFFB3261E)
private val md_onError_light = Color(0xFFFFFFFF)
private val md_errorContainer_light = Color(0xFFF9DEDC)
private val md_onErrorContainer_light = Color(0xFF8C1D18)
private val md_surface_light = Color(0xFFFEF7FF)
private val md_onSurface_light = Color(0xFF1D1B20)
private val md_surfaceVariant_light = Color(0xFFE7E0EC)
private val md_onSurfaceVariant_light = Color(0xFF49454F)
private val md_outline_light = Color(0xFF79747E)
private val md_outlineVariant_light = Color(0xFFCAC4D0)
private val md_inverseSurface_light = Color(0xFF322F35)
private val md_inverseOnSurface_light = Color(0xFFF5EFF7)
private val md_surfaceContainer_light = Color(0xFFF3EDF7)
private val md_surfaceContainerLow_light = Color(0xFFF7F2FA)
private val md_surfaceContainerHigh_light = Color(0xFFECE6F0)
private val md_surfaceContainerHighest_light = Color(0xFFE6E0E9)

private val md_primary_dark = Color(0xFFD0BCFF)
private val md_onPrimary_dark = Color(0xFF381E72)
private val md_primaryContainer_dark = Color(0xFF4F378B)
private val md_onPrimaryContainer_dark = Color(0xFFEADDFF)
private val md_secondary_dark = Color(0xFFCCC2DC)
private val md_onSecondary_dark = Color(0xFF332D41)
private val md_secondaryContainer_dark = Color(0xFF4A4458)
private val md_onSecondaryContainer_dark = Color(0xFFE8DEF8)
private val md_tertiary_dark = Color(0xFFEFB8C8)
private val md_onTertiary_dark = Color(0xFF492532)
private val md_tertiaryContainer_dark = Color(0xFF633B48)
private val md_onTertiaryContainer_dark = Color(0xFFFFD8E4)
private val md_error_dark = Color(0xFFF2B8B5)
private val md_onError_dark = Color(0xFF601410)
private val md_errorContainer_dark = Color(0xFF8C1D18)
private val md_onErrorContainer_dark = Color(0xFFF9DEDC)
private val md_surface_dark = Color(0xFF141218)
private val md_onSurface_dark = Color(0xFFE6E0E9)
private val md_surfaceVariant_dark = Color(0xFF49454F)
private val md_onSurfaceVariant_dark = Color(0xFFCAC4D0)
private val md_outline_dark = Color(0xFF938F99)
private val md_outlineVariant_dark = Color(0xFF49454F)
private val md_inverseSurface_dark = Color(0xFFE6E0E9)
private val md_inverseOnSurface_dark = Color(0xFF322F35)
private val md_surfaceContainer_dark = Color(0xFF211F26)
private val md_surfaceContainerLow_dark = Color(0xFF1D1B20)
private val md_surfaceContainerHigh_dark = Color(0xFF2B2930)
private val md_surfaceContainerHighest_dark = Color(0xFF36343B)

private val LightColorScheme = lightColorScheme(
    primary = md_primary_light,
    onPrimary = md_onPrimary_light,
    primaryContainer = md_primaryContainer_light,
    onPrimaryContainer = md_onPrimaryContainer_light,
    secondary = md_secondary_light,
    onSecondary = md_onSecondary_light,
    secondaryContainer = md_secondaryContainer_light,
    onSecondaryContainer = md_onSecondaryContainer_light,
    tertiary = md_tertiary_light,
    onTertiary = md_onTertiary_light,
    tertiaryContainer = md_tertiaryContainer_light,
    onTertiaryContainer = md_onTertiaryContainer_light,
    error = md_error_light,
    onError = md_onError_light,
    errorContainer = md_errorContainer_light,
    onErrorContainer = md_onErrorContainer_light,
    surface = md_surface_light,
    onSurface = md_onSurface_light,
    surfaceVariant = md_surfaceVariant_light,
    onSurfaceVariant = md_onSurfaceVariant_light,
    outline = md_outline_light,
    outlineVariant = md_outlineVariant_light,
    inverseSurface = md_inverseSurface_light,
    inverseOnSurface = md_inverseOnSurface_light,
    surfaceContainer = md_surfaceContainer_light,
    surfaceContainerLow = md_surfaceContainerLow_light,
    surfaceContainerHigh = md_surfaceContainerHigh_light,
    surfaceContainerHighest = md_surfaceContainerHighest_light,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_primary_dark,
    onPrimary = md_onPrimary_dark,
    primaryContainer = md_primaryContainer_dark,
    onPrimaryContainer = md_onPrimaryContainer_dark,
    secondary = md_secondary_dark,
    onSecondary = md_onSecondary_dark,
    secondaryContainer = md_secondaryContainer_dark,
    onSecondaryContainer = md_onSecondaryContainer_dark,
    tertiary = md_tertiary_dark,
    onTertiary = md_onTertiary_dark,
    tertiaryContainer = md_tertiaryContainer_dark,
    onTertiaryContainer = md_onTertiaryContainer_dark,
    error = md_error_dark,
    onError = md_onError_dark,
    errorContainer = md_errorContainer_dark,
    onErrorContainer = md_onErrorContainer_dark,
    surface = md_surface_dark,
    onSurface = md_onSurface_dark,
    surfaceVariant = md_surfaceVariant_dark,
    onSurfaceVariant = md_onSurfaceVariant_dark,
    outline = md_outline_dark,
    outlineVariant = md_outlineVariant_dark,
    inverseSurface = md_inverseSurface_dark,
    inverseOnSurface = md_inverseOnSurface_dark,
    surfaceContainer = md_surfaceContainer_dark,
    surfaceContainerLow = md_surfaceContainerLow_dark,
    surfaceContainerHigh = md_surfaceContainerHigh_dark,
    surfaceContainerHighest = md_surfaceContainerHighest_dark,
)

@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
