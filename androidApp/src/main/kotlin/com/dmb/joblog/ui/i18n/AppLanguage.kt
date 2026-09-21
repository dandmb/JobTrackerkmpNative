package com.dmb.joblog.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.dmb.joblog.i18n.AppLanguage
import java.util.Locale

/**
 * Langue de l'interface pour le contenu PARTAGÉ (onboarding, « À propos », dates courtes, message de validation) :
 * français si la langue du système (ou la langue choisie pour l'app, Android 13+) est le français, anglais sinon.
 * Les libellés statiques, eux, suivent la résolution standard des ressources (`values/` = anglais, `values-fr/` = français).
 * Lue dans la configuration Compose : un changement de langue relance la composition.
 */
@Composable
fun rememberAppLanguage(): AppLanguage {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return remember(locale) { languageOf(locale) }
}

/** Version hors composition (ex. contexte Android brut). */
fun currentAppLanguage(): AppLanguage = languageOf(Locale.getDefault())

internal fun languageOf(locale: Locale): AppLanguage = AppLanguage.fromTag(locale.toLanguageTag())
