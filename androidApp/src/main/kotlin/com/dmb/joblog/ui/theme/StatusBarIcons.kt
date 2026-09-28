package com.dmb.joblog.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Couleur des icônes de la barre d'état (heure, batterie…) pour un écran dont la barre du haut est en couleur `primary` :
 * en thème sombre `primary` est un teal CLAIR (Teal80) → icônes SOMBRES ; en thème clair c'est un teal FONCÉ (Teal40) →
 * icônes CLAIRES. Sans cela, `enableEdgeToEdge()` suit seulement le thème système et affichait des icônes blanches sur
 * fond teal clair en mode sombre (illisibles). L'état précédent est restauré quand l'écran quitte la composition.
 */
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
