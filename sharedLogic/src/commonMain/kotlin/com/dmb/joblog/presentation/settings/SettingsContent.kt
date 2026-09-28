package com.dmb.joblog.presentation.settings

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick

/** Contenu de l'écran « Réglages », source unique Android/iOS : point d'entrée vers « À propos » et « Politique de confidentialité ». */
class SettingsContent private constructor(private val lang: AppLanguage) {
    val screenTitle: String = lang.pick(en = "Settings", fr = "Réglages")
    val backLabel: String = lang.pick(en = "Back", fr = "Retour")

    val entryPointLabel: String = screenTitle

    val aboutRowLabel: String = lang.pick(en = "About", fr = "À propos")
    val privacyRowLabel: String = lang.pick(en = "Privacy Policy", fr = "Politique de confidentialité")

    companion object {
        fun of(language: AppLanguage): SettingsContent = SettingsContent(language)
    }
}
