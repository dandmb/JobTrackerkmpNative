package com.dmb.jobtracker.presentation.about

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
 * Contenu de l'écran « À propos » : UNE source de vérité pour Android et iOS (comme `OnboardingContent`).
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
object AboutContent {
    const val SCREEN_TITLE = "À propos"
    const val APP_NAME = "JobTracker"
    const val TAGLINE = "Suis tes candidatures, à ton rythme."
    const val BACK_LABEL = "Retour"
    const val ENTRY_POINT_LABEL = "À propos"

    /** « Version 1.0 (1) » : `versionName` et numéro de build lus par chaque plateforme dans sa configuration de build. */
    fun versionLabel(versionName: String, buildNumber: String): String {
        val version = versionName.trim().ifEmpty { "?" }
        val build = buildNumber.trim()
        return if (build.isEmpty()) "Version $version" else "Version $version ($build)"
    }

    val sections: List<AboutSection> = listOf(
        AboutSection(
            title = "Ce que l'app enregistre",
            intro = "JobTracker ne garde que ce que tu saisis toi-même dans une candidature :",
            bullets = listOf(
                "le titre du poste et l'entreprise ;",
                "la localisation, la source (LinkedIn, cooptation…) et le lien de l'annonce ;",
                "la fourchette de salaire ;",
                "les dates de candidature, d'entretien et de résultat ;",
                "le statut de la candidature ;",
                "tes notes.",
            ),
            note = "L'app y ajoute la date et l'heure de création de chaque candidature (pour ordonner la liste) " +
                "et retient que tu as déjà vu l'écran de bienvenue. Elle ne te demande ni nom, ni e-mail, ni compte.",
        ),
        AboutSection(
            title = "Où vivent tes données",
            bullets = listOf(
                "Tout est stocké dans une base de données locale, sur ton appareil.",
                "L'app ne contient aucun code qui communique avec un serveur : tes candidatures ne sont envoyées nulle part.",
                "Aucun compte n'est nécessaire et rien n'est partagé avec des tiers par l'app.",
                "Seule exception, sur Android : la police d'écriture est fournie par les services Google Play, " +
                    "qui la téléchargent. Cette demande ne concerne que la police, jamais tes candidatures.",
            ),
            note = "Le système de ton téléphone peut, lui, sauvegarder les données des applications " +
                "(sauvegarde Google sur Android, iCloud ou sauvegarde de l'appareil sur iOS) selon tes réglages : " +
                "ce mécanisme est géré par le système, pas par l'app.",
        ),
        AboutSection(
            title = "Ce que l'app ne fait pas",
            bullets = listOf(
                "Pas de suivi de ton usage : aucun outil d'analyse ni de statistiques n'est intégré.",
                "Pas de publicité.",
                "Pas de compte, pas d'identifiant publicitaire.",
                "Aucune autorisation sensible demandée (position, contacts, photos, micro…).",
            ),
        ),
        AboutSection(
            title = "Tes droits sur tes données",
            intro = "Tes données sont les tiennes, et tu les contrôles à tout moment, directement dans l'app :",
            bullets = listOf(
                "les consulter : toutes tes candidatures sont dans la liste principale ;",
                "les modifier : le bouton d'édition de chaque candidature ;",
                "supprimer une candidature : glisse-la sur le côté ;",
                "tout supprimer : le bouton « $DELETE_ALL_LABEL » ci-dessous efface toutes tes candidatures de l'appareil.",
            ),
            note = "Une sauvegarde déjà réalisée par le système de ton téléphone (voir plus haut) n'est pas effacée par cette action.",
        ),
    )

    // ---- Suppression de toutes les données ----

    const val DELETE_ALL_LABEL = "Supprimer toutes mes données"
    const val DELETE_ALL_EXPLANATION =
        "Efface définitivement toutes tes candidatures de cet appareil. Cette action est irréversible."
    const val DELETE_ALL_SUCCESS_MESSAGE = "Toutes tes données ont été supprimées."
    const val DELETE_ALL_IN_PROGRESS_LABEL = "Suppression en cours…"

    val firstConfirmation = ConfirmationTexts(
        title = "Supprimer toutes tes données ?",
        message = "Toutes tes candidatures seront effacées de cet appareil. Tu ne pourras pas les récupérer.",
        confirmLabel = "Continuer",
        cancelLabel = "Annuler",
    )

    val finalConfirmation = ConfirmationTexts(
        title = "Dernière confirmation",
        message = "Tout va être supprimé définitivement, sans retour en arrière possible. Veux-tu vraiment tout effacer ?",
        confirmLabel = "Tout supprimer",
        cancelLabel = "Annuler",
    )

    // ---- Contact ----

    const val CONTACT_TITLE = "Nous contacter"
    const val CONTACT_INTRO = "Une question, un retour ? Écris-nous :"
    const val CONTACT_LABEL = "Nous contacter"
    const val CONTACT_EMAIL = "bizwadan@gmail.com"
    const val CONTACT_SUBJECT = "JobTracker - Contact"
    const val CONTACT_NOTE =
        "Ton application de messagerie s'ouvre avec un message pré-rempli : rien n'est envoyé tant que tu ne l'envoies pas toi-même."
    const val CONTACT_NO_MAIL_APP_MESSAGE =
        "Aucune application de messagerie n'a pu s'ouvrir. Tu peux nous écrire à $CONTACT_EMAIL."

    /**
     * Adresse `mailto:` du lien de contact, identique sur les deux plateformes :
     * `mailto:bizwadan@gmail.com?subject=JobTracker%20-%20Contact` (objet pré-rempli pour faciliter le tri des messages).
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
