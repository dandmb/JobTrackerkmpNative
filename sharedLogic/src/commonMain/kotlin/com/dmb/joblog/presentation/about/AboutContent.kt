package com.dmb.joblog.presentation.about

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick

/**
 * Une section de l'écran « À propos » : titre, phrase d'introduction facultative, liste à puces facultative, note facultative.
 */
data class AboutSection(
    val title: String,
    val intro: String? = null,
    val bullets: List<String> = emptyList(),
    val note: String? = null,
)

/** Textes d'une boîte de dialogue de confirmation. */
data class ConfirmationTexts(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val cancelLabel: String,
)

/**
 * Contenu de l'écran « À propos » : UNE source de vérité pour Android et iOS (comme `OnboardingContent`), dans la langue demandée ([AppLanguage]).
 *
 * Aucune licence de dépendance ni de police n'est mentionnée à l'écran : décision explicite du propriétaire (ne pas la rétablir).
 * Chaque affirmation ci-dessous a été vérifiée dans le code du projet ; la liste des vérifications est dans
 * `rapport-a-propos.md` et `PROJECT_CONTEXT.md`. **Si le code change (nouveau champ de `JobOffer`, SDK d'analyse,
 * appel réseau, nouvelle permission…), ce texte doit être revu** : `AboutContentTest` verrouille la liste des champs
 * du modèle pour que l'ajout d'un champ fasse échouer un test et rappelle de mettre le texte à jour.
 *
 * Texte informatif de bonne foi, pas un document juridique : une publication sur un store demandera une vraie
 * politique de confidentialité hébergée en ligne.
 */
class AboutContent private constructor(private val lang: AppLanguage) {
    val screenTitle: String = lang.pick(en = "About", fr = "À propos")
    val tagline: String = lang.pick(en = "Track your applications, at your own pace.", fr = "Suis tes candidatures, à ton rythme.")
    val backLabel: String = lang.pick(en = "Back", fr = "Retour")
    val entryPointLabel: String = screenTitle

    /** « Version 1.0 (1) » : `versionName` et numéro de build lus par chaque plateforme dans sa configuration de build. */
    fun versionLabel(versionName: String, buildNumber: String): String {
        val version = versionName.trim().ifEmpty { "?" }
        val build = buildNumber.trim()
        return if (build.isEmpty()) "Version $version" else "Version $version ($build)"
    }

    // ---- Suppression de toutes les données (libellé utilisé aussi dans la section « droits ») ----

    val deleteAllLabel: String = lang.pick(en = "Delete all my data", fr = "Supprimer toutes mes données")
    val deleteAllExplanation: String = lang.pick(
        en = "Permanently erases all your applications from this device. This action cannot be undone.",
        fr = "Efface définitivement toutes tes candidatures de cet appareil. Cette action est irréversible.",
    )
    val deleteAllSuccessMessage: String = lang.pick(en = "All your data has been deleted.", fr = "Toutes tes données ont été supprimées.")
    val deleteAllInProgressLabel: String = lang.pick(en = "Deleting…", fr = "Suppression en cours…")
    val deleteAllFailedMessage: String = lang.pick(
        en = "Deletion failed. Your data was not deleted, please try again.",
        fr = "La suppression a échoué. Tes données n'ont pas été supprimées, réessaie.",
    )

    val firstConfirmation = ConfirmationTexts(
        title = lang.pick(en = "Delete all your data?", fr = "Supprimer toutes tes données ?"),
        message = lang.pick(
            en = "All your applications will be erased from this device. You will not be able to recover them.",
            fr = "Toutes tes candidatures seront effacées de cet appareil. Tu ne pourras pas les récupérer.",
        ),
        confirmLabel = lang.pick(en = "Continue", fr = "Continuer"),
        cancelLabel = lang.pick(en = "Cancel", fr = "Annuler"),
    )

    val finalConfirmation = ConfirmationTexts(
        title = lang.pick(en = "Final confirmation", fr = "Dernière confirmation"),
        message = lang.pick(
            en = "Everything will be permanently deleted, with no way back. Do you really want to erase everything?",
            fr = "Tout va être supprimé définitivement, sans retour en arrière possible. Veux-tu vraiment tout effacer ?",
        ),
        confirmLabel = lang.pick(en = "Delete everything", fr = "Tout supprimer"),
        cancelLabel = lang.pick(en = "Cancel", fr = "Annuler"),
    )

    val sections: List<AboutSection> = listOf(
        AboutSection(
            title = lang.pick(en = "What the app stores", fr = "Ce que l'app enregistre"),
            intro = lang.pick(
                en = "JobLog only keeps what you enter yourself in an application:",
                fr = "JobLog ne garde que ce que tu saisis toi-même dans une candidature :",
            ),
            bullets = listOf(
                lang.pick(en = "the job title and the company;", fr = "le titre du poste et l'entreprise ;"),
                lang.pick(
                    en = "the location, the source (LinkedIn, referral…) and the link to the job posting;",
                    fr = "la localisation, la source (LinkedIn, cooptation…) et le lien de l'annonce ;",
                ),
                lang.pick(en = "the salary range;", fr = "la fourchette de salaire ;"),
                lang.pick(en = "the application, interview and result dates;", fr = "les dates de candidature, d'entretien et de résultat ;"),
                lang.pick(en = "the application status;", fr = "le statut de la candidature ;"),
                lang.pick(en = "your notes.", fr = "tes notes."),
            ),
            note = lang.pick(
                en = "The app adds the date and time each application was created (to order the list) " +
                    "and remembers that you have already seen the welcome screen. It does not ask for your name, e-mail or an account.",
                fr = "L'app y ajoute la date et l'heure de création de chaque candidature (pour ordonner la liste) " +
                    "et retient que tu as déjà vu l'écran de bienvenue. Elle ne te demande ni nom, ni e-mail, ni compte.",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "Where your data lives", fr = "Où vivent tes données"),
            bullets = listOf(
                lang.pick(
                    en = "Everything is stored in a local database, on your device.",
                    fr = "Tout est stocké dans une base de données locale, sur ton appareil.",
                ),
                lang.pick(
                    en = "The app contains no code that talks to a server: your applications are not sent anywhere.",
                    fr = "L'app ne contient aucun code qui communique avec un serveur : tes candidatures ne sont envoyées nulle part.",
                ),
                lang.pick(
                    en = "No account is needed and the app shares nothing with third parties.",
                    fr = "Aucun compte n'est nécessaire et rien n'est partagé avec des tiers par l'app.",
                ),
                lang.pick(
                    en = "The only exception, on Android: the font is provided by Google Play services, which download it. " +
                        "That request only concerns the font, never your applications.",
                    fr = "Seule exception, sur Android : la police d'écriture est fournie par les services Google Play, " +
                        "qui la téléchargent. Cette demande ne concerne que la police, jamais tes candidatures.",
                ),
            ),
            note = lang.pick(
                en = "Your phone's system may back up app data (Google backup on Android, iCloud or device backup on iOS) " +
                    "depending on your settings: that mechanism is managed by the system, not by the app.",
                fr = "Le système de ton téléphone peut, lui, sauvegarder les données des applications " +
                    "(sauvegarde Google sur Android, iCloud ou sauvegarde de l'appareil sur iOS) selon tes réglages : " +
                    "ce mécanisme est géré par le système, pas par l'app.",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "What the app does not do", fr = "Ce que l'app ne fait pas"),
            bullets = listOf(
                lang.pick(
                    en = "No usage tracking: no analytics or statistics tool is built in.",
                    fr = "Pas de suivi de ton usage : aucun outil d'analyse ni de statistiques n'est intégré.",
                ),
                lang.pick(en = "No ads.", fr = "Pas de publicité."),
                lang.pick(en = "No account, no advertising identifier.", fr = "Pas de compte, pas d'identifiant publicitaire."),
                lang.pick(
                    en = "No sensitive permission requested (location, contacts, photos, microphone…).",
                    fr = "Aucune autorisation sensible demandée (position, contacts, photos, micro…).",
                ),
            ),
        ),
        AboutSection(
            title = lang.pick(en = "Your rights over your data", fr = "Tes droits sur tes données"),
            intro = lang.pick(
                en = "Your data is yours, and you control it at any time, right in the app:",
                fr = "Tes données sont les tiennes, et tu les contrôles à tout moment, directement dans l'app :",
            ),
            bullets = listOf(
                lang.pick(
                    en = "view it: all your applications are in the main list;",
                    fr = "les consulter : toutes tes candidatures sont dans la liste principale ;",
                ),
                lang.pick(en = "edit it: the edit button on each application;", fr = "les modifier : le bouton d'édition de chaque candidature ;"),
                lang.pick(en = "delete an application: swipe it sideways;", fr = "supprimer une candidature : glisse-la sur le côté ;"),
                lang.pick(
                    en = "delete everything: the “$deleteAllLabel” button below erases all your applications from the device.",
                    fr = "tout supprimer : le bouton « $deleteAllLabel » ci-dessous efface toutes tes candidatures de l'appareil.",
                ),
            ),
            note = lang.pick(
                en = "A backup already made by your phone's system (see above) is not erased by this action.",
                fr = "Une sauvegarde déjà réalisée par le système de ton téléphone (voir plus haut) n'est pas effacée par cette action.",
            ),
        ),
    )

    // ---- Contact ----

    val contactTitle: String = lang.pick(en = "Contact us", fr = "Nous contacter")
    val contactIntro: String = lang.pick(en = "A question, some feedback? Write to us:", fr = "Une question, un retour ? Écris-nous :")
    val contactLabel: String = contactTitle
    val contactNote: String = lang.pick(
        en = "Your mail app opens with a pre-filled message: nothing is sent until you send it yourself.",
        fr = "Ton application de messagerie s'ouvre avec un message pré-rempli : rien n'est envoyé tant que tu ne l'envoies pas toi-même.",
    )
    val contactNoMailAppMessage: String = lang.pick(
        en = "No mail app could be opened. You can write to us at $CONTACT_EMAIL.",
        fr = "Aucune application de messagerie n'a pu s'ouvrir. Tu peux nous écrire à $CONTACT_EMAIL.",
    )

    companion object {
        /** Contenu dans la langue demandée. */
        fun of(language: AppLanguage): AboutContent = AboutContent(language)

        // Constantes indépendantes de la langue.
        const val APP_NAME = "JobLog"
        const val CONTACT_EMAIL = "bizwadan@gmail.com"
        const val CONTACT_SUBJECT = "JobLog - Contact"

        /**
         * Adresse `mailto:` du lien de contact, identique sur les deux plateformes et dans les deux langues :
         * `mailto:bizwadan@gmail.com?subject=JobLog%20-%20Contact` (objet pré-rempli pour faciliter le tri des messages).
         */
        fun contactMailtoUri(): String = "mailto:$CONTACT_EMAIL?subject=${percentEncode(CONTACT_SUBJECT)}"

        /** Encodage pourcent (RFC 3986) en UTF-8 : seuls `A-Z a-z 0-9 - . _ ~` restent tels quels. */
        internal fun percentEncode(text: String): String = buildString {
            for (byte in text.encodeToByteArray()) {
                val value = byte.toInt() and 0xFF
                val char = value.toChar()
                if (char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char == '-' || char == '.' || char == '_' || char == '~') {
                    append(char)
                } else {
                    append('%')
                    append(HEX[value shr 4])
                    append(HEX[value and 0x0F])
                }
            }
        }

        private const val HEX = "0123456789ABCDEF"
    }
}
