package com.dmb.joblog.presentation.settings

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick

/**
 * Contenu de l'écran « Réglages » : UNE source de vérité pour Android et iOS (comme `AboutContent`, `OnboardingContent`).
 * Cet écran est le point d'entrée unique vers « À propos » et « Politique de confidentialité », atteint depuis l'icône
 * ⚙️ de la barre du haut de l'écran principal (avant cette intervention, l'icône ⓘ menait directement à « À propos »,
 * qui contenait aussi la confidentialité).
 */
class SettingsContent private constructor(private val lang: AppLanguage) {
    val screenTitle: String = lang.pick(en = "Settings", fr = "Réglages")
    val backLabel: String = lang.pick(en = "Back", fr = "Retour")

    /** Contenu de l'icône ⚙️ dans la barre du haut de la liste (remplace l'ancien point d'entrée direct vers « À propos »). */
    val entryPointLabel: String = screenTitle

    val aboutRowLabel: String = lang.pick(en = "About", fr = "À propos")
    val privacyRowLabel: String = lang.pick(en = "Privacy Policy", fr = "Politique de confidentialité")

    companion object {
        fun of(language: AppLanguage): SettingsContent = SettingsContent(language)
    }
}
