package com.diu.yk_games.line2box.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = scrim,
    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    inversePrimary = inversePrimary,
    surfaceDim = surfaceDim,
    surfaceBright = surfaceBright,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest
)

private val ChatScheme = lightColorScheme(
    primary = primaryChat,
    onPrimary = onPrimaryChat,
    primaryContainer = primaryContainerChat,
    onPrimaryContainer = onPrimaryContainerChat,
    secondary = secondaryChat,
    onSecondary = onSecondaryChat,
    secondaryContainer = secondaryContainerChat,
    onSecondaryContainer = onSecondaryContainerChat,
    tertiary = tertiaryChat,
    onTertiary = onTertiaryChat,
    tertiaryContainer = tertiaryContainerChat,
    onTertiaryContainer = onTertiaryContainerChat,
    error = errorChat,
    onError = onErrorChat,
    errorContainer = errorContainerChat,
    onErrorContainer = onErrorContainerChat,
    background = backgroundChat,
    onBackground = onBackgroundChat,
    surface = surfaceChat,
    onSurface = onSurfaceChat,
    surfaceVariant = surfaceVariantChat,
    onSurfaceVariant = onSurfaceVariantChat,
    outline = outlineChat,
    outlineVariant = outlineVariantChat,
    scrim = scrimChat,
    inverseSurface = inverseSurfaceChat,
    inverseOnSurface = inverseOnSurfaceChat,
    inversePrimary = inversePrimaryChat,
    surfaceDim = surfaceDimChat,
    surfaceBright = surfaceBrightChat,
    surfaceContainerLowest = surfaceContainerLowestChat,
    surfaceContainerLow = surfaceContainerLowChat,
    surfaceContainer = surfaceContainerChat,
    surfaceContainerHigh = surfaceContainerHighChat,
    surfaceContainerHighest = surfaceContainerHighestChat
)

@Composable
fun Line2BoxTheme(
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = DarkScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun Line2BoxChatTheme(
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = ChatScheme,
        typography = Typography,
        content = content
    )
}