package dev.panthu.sololife.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary           = PrimaryDark,
    onPrimary         = OnPrimaryDark,
    primaryContainer  = PrimaryContDark,
    onPrimaryContainer= OnPrimaryContDark,
    secondary         = SecondaryDark,
    onSecondary       = OnSecondaryDark,
    secondaryContainer= SecondaryContDark,
    tertiary          = TertiaryDark,
    onTertiary        = OnTertiaryDark,
    error             = ErrorDark,
    onError           = OnErrorDark,
    background        = DarkBackground,
    onBackground      = OnBackgroundDark,
    surface           = DarkSurface,
    onSurface         = OnSurfaceDark,
    surfaceVariant    = DarkSurfaceVar,
    onSurfaceVariant  = OnSurfaceVarDark,
    outline           = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary           = PrimaryLight,
    onPrimary         = OnPrimaryLight,
    primaryContainer  = PrimaryContLight,
    onPrimaryContainer= OnPrimaryContLight,
    secondary         = SecondaryLight,
    onSecondary       = OnSecondaryLight,
    secondaryContainer= SecondaryContLight,
    tertiary          = TertiaryLight,
    onTertiary        = OnTertiaryLight,
    error             = ErrorLight,
    onError           = OnErrorLight,
    background        = LightBackground,
    onBackground      = OnBackgroundLight,
    surface           = LightSurface,
    onSurface         = OnSurfaceLight,
    surfaceVariant    = LightSurfaceVar,
    onSurfaceVariant  = OnSurfaceVarLight,
    outline           = LightOutline
)

@Composable
fun SoloLifeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
