package com.dmb.joblog.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// En thème sombre, `primary` est un teal CLAIR : sans forcer des icônes sombres, `enableEdgeToEdge()` laisse les
// icônes de la barre d'état blanches sur ce fond clair (illisibles).
@Composable
fun StatusBarIconsForPrimaryTopBar() {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    if (view.isInEditMode) return
    DisposableEffect(darkTheme) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previous = controller.isAppearanceLightStatusBars
            controller.isAppearanceLightStatusBars = darkTheme
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
