package com.factory.solacecalmsolitaire.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SolaceColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = FeltGreenDark,
    secondary = MutedTeal,
    onSecondary = FeltGreenDark,
    background = DarkBackground,
    onBackground = SoftCream,
    surface = DarkSurface,
    onSurface = SoftCream,
    surfaceVariant = FeltGreenSurface,
    onSurfaceVariant = SoftCream,
    error = CardRed,
    onError = SoftCream
)

/**
 * The felt table, card faces, and card backs always use Solace's own dark palette so the
 * game keeps its identity regardless of theme. [dynamicColor] only affects chrome drawn by
 * Material 3 components (sheets, dialogs, buttons) — on Android 12+ it derives that chrome
 * from the user's wallpaper (Material You); elsewhere it falls back to [SolaceColorScheme].
 */
@Composable
fun SolaceCalmSolitaireTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        SolaceColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SolaceTypography,
        content = content
    )
}
