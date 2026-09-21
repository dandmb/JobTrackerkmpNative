package com.dmb.joblog.i18n

/**
 * Langues de l'interface : **anglais par défaut**, français si la langue du système est le français. Toute autre langue
 * système retombe sur l'anglais (aucune troisième langue pour l'instant).
 *
 * Pourquoi un type maison et pas une bibliothèque d'i18n KMP : voir `PROJECT_CONTEXT.md` (§ « Internationalisation »).
 * En bref : les écrans sont natifs (Compose / SwiftUI) et lisent leurs libellés statiques dans les ressources natives de leur
 * plateforme (`strings.xml`, `Localizable.strings`) ; ce type ne sert qu'au contenu et aux règles PARTAGÉS (onboarding,
 * « À propos », messages de validation, formats de date), pour qu'ils restent en source unique. Chaque plateforme détecte
 * la langue avec son mécanisme standard et la passe à `fromTag`.
 */
enum class AppLanguage {
    EN, FR;

    companion object {
        val DEFAULT: AppLanguage = EN

        /**
         * Langue à partir d'une balise de langue (BCP 47 : « fr », « fr-CA », « fr_FR », « en-US »…) ou d'un identifiant de
         * localisation : `fr…` → [FR], tout le reste (y compris `null`, vide ou balise inconnue) → [EN].
         */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.trim()?.lowercase()?.split('-', '_')?.firstOrNull().orEmpty()
            return if (language == "fr") FR else DEFAULT
        }
    }
}

/** Choisit le texte de la langue : les deux traductions sont écrites côte à côte, donc une traduction ne peut pas être oubliée. */
internal fun AppLanguage.pick(en: String, fr: String): String = if (this == AppLanguage.FR) fr else en
