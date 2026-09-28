package com.dmb.joblog.presentation.privacy

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick
import com.dmb.joblog.presentation.about.AboutSection

/**
 * ⚠️ Texte français VERBATIM (approuvé par le propriétaire, identique au Gist [ONLINE_URL]) : ne jamais le reformuler
 * — verrouillé par `PrivacyContentTest`. Texte anglais = traduction non officielle.
 */
class PrivacyContent private constructor(private val lang: AppLanguage) {
    val screenTitle: String = lang.pick(en = "Privacy Policy", fr = "Politique de confidentialité")
    val backLabel: String = lang.pick(en = "Back", fr = "Retour")

    val documentTitle: String = lang.pick(en = "Privacy Policy — JobLog", fr = "Politique de confidentialité — JobLog")
    val lastUpdatedLabel: String = lang.pick(en = "Last updated: September 27, 2026", fr = "Dernière mise à jour : 27 septembre 2026")
    val introText: String = lang.pick(
        en = "JobLog is a job application tracking app developed by Dan Bizwa. This policy explains what data the app processes and how.",
        fr = "JobLog est une application de suivi de candidatures développée par Dan Bizwa. Cette politique explique quelles données l'application traite et comment.",
    )

    val sections: List<AboutSection> = listOf(
        AboutSection(
            title = lang.pick(en = "1. Summary", fr = "1. Résumé"),
            intro = lang.pick(
                en = "JobLog runs entirely on your device. The app does not collect, transmit or share any data with its developers, " +
                    "with third parties or with a server: it doesn't have one.",
                fr = "JobLog fonctionne entièrement sur ton appareil. L'application ne collecte, ne transmet et ne partage aucune donnée " +
                    "avec ses développeurs, avec des tiers ou avec un serveur : elle n'en a pas.",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "2. Data recorded", fr = "2. Données enregistrées"),
            intro = lang.pick(
                en = "The app only records what you enter yourself in an application:",
                fr = "L'application enregistre uniquement ce que tu saisis toi-même dans une candidature :",
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
                en = "The app automatically adds the date and time each application was created, to order the list, and remembers " +
                    "whether you have already seen the welcome screen. It does not ask for your name, e-mail address or an account.",
                fr = "L'application ajoute automatiquement la date et l'heure de création de chaque candidature, pour ordonner la liste, " +
                    "et retient si tu as déjà vu l'écran de bienvenue. Elle ne te demande ni nom, ni adresse e-mail, ni création de compte.",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "3. Where your data is stored", fr = "3. Où sont stockées tes données"),
            intro = lang.pick(
                en = "All your data is stored in a local database, on your device. The app contains no code that communicates with a " +
                    "server: your applications are not sent anywhere, neither to Dan Bizwa nor to a third party.",
                fr = "Toutes tes données sont stockées dans une base de données locale, sur ton appareil. L'application ne contient aucun " +
                    "code qui communique avec un serveur : tes candidatures ne sont envoyées nulle part, ni à Dan Bizwa, ni à un tiers.",
            ),
            note = lang.pick(
                en = "Exception, on Android only: the app's font is provided by Google Play services, which download it on first " +
                    "launch. This network request only concerns the font; it does not transmit any of your applications or any " +
                    "personal data.\n\nSystem backups: depending on your device's settings, the operating system (Google backup on " +
                    "Android, iCloud or local backup on iOS) may include the app's data in a general backup of your phone. This " +
                    "mechanism is managed by the operating system, not by JobLog, and is outside the app's control.",
                fr = "Exception, sur Android uniquement : la police d'écriture de l'application est fournie par les services Google " +
                    "Play, qui la téléchargent lors du premier lancement. Cette requête réseau ne concerne que la police d'écriture ; " +
                    "elle ne transmet aucune de tes candidatures ni aucune donnée personnelle.\n\nSauvegardes système : selon les " +
                    "réglages de ton appareil, le système d'exploitation (sauvegarde Google sur Android, iCloud ou sauvegarde locale " +
                    "sur iOS) peut inclure les données de l'application dans une sauvegarde générale de ton téléphone. Ce mécanisme " +
                    "est géré par le système d'exploitation, pas par JobLog, et échappe au contrôle de l'application.",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "4. What the app does not do", fr = "4. Ce que l'application ne fait pas"),
            bullets = listOf(
                lang.pick(en = "No usage tracking or analytics tool is built in.", fr = "Aucun outil d'analyse d'usage ou de statistiques n'est intégré."),
                lang.pick(en = "No ads.", fr = "Aucune publicité."),
                lang.pick(en = "No user account, no advertising identifier.", fr = "Aucun compte utilisateur, aucun identifiant publicitaire."),
                lang.pick(
                    en = "No sensitive permission is requested (location, contacts, photos, microphone…).",
                    fr = "Aucune autorisation sensible n'est demandée (localisation, contacts, photos, micro…).",
                ),
                lang.pick(en = "No data sharing with third parties.", fr = "Aucun partage de données avec des tiers."),
            ),
        ),
        AboutSection(
            title = lang.pick(en = "5. Your rights over your data", fr = "5. Tes droits sur tes données"),
            intro = lang.pick(
                en = "Your data belongs to you and stays under your direct control, in the app:",
                fr = "Tes données t'appartiennent et restent sous ton contrôle direct, dans l'application :",
            ),
            bullets = listOf(
                lang.pick(
                    en = "View and edit: accessible at any time from your list of applications.",
                    fr = "Consultation et modification : accessibles à tout moment depuis la liste de tes candidatures.",
                ),
                lang.pick(
                    en = "Individual deletion: each application can be deleted separately.",
                    fr = "Suppression individuelle : chaque candidature peut être supprimée séparément.",
                ),
                lang.pick(
                    en = "Full deletion: the “About” screen offers an option to immediately and permanently delete all your data from the app.",
                    fr = "Suppression totale : l'écran « À propos » propose une option pour supprimer immédiatement et intégralement " +
                        "toutes tes données de l'application.",
                ),
            ),
            note = lang.pick(
                en = "Since no data is stored anywhere other than your device, these actions are enough to completely erase your " +
                    "information, except for a system backup already made before deletion (see section 3).",
                fr = "Comme aucune donnée n'est stockée ailleurs que sur ton appareil, ces actions suffisent à effacer complètement " +
                    "tes informations, à l'exception d'une sauvegarde système déjà réalisée avant la suppression (voir section 3).",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "6. Minors", fr = "6. Mineurs"),
            intro = lang.pick(
                en = "JobLog does not collect any personally identifiable data and does not specifically target minors. The app " +
                    "does not ask for any information that would identify the user's age.",
                fr = "JobLog ne collecte aucune donnée personnelle identifiable et ne cible pas spécifiquement les mineurs. " +
                    "L'application ne demande aucune information permettant d'identifier l'âge de l'utilisateur.",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "7. Changes to this policy", fr = "7. Modifications de cette politique"),
            intro = lang.pick(
                en = "This policy may be updated if the app evolves (for example, if a new feature involved different data " +
                    "processing). The date of the last update appears at the top of this document. Any significant change will be " +
                    "reflected here before being published in a new version of the app.",
                fr = "Cette politique peut être mise à jour si l'application évolue (par exemple, si une nouvelle fonctionnalité " +
                    "impliquait un traitement de données différent). La date de dernière mise à jour figure en haut de ce document. " +
                    "Toute évolution significative sera reflétée ici avant d'être publiée dans une nouvelle version de l'application.",
            ),
        ),
        AboutSection(
            title = lang.pick(en = "8. Contact", fr = "8. Contact"),
            intro = lang.pick(
                en = "For any question about this policy or your data, contact: $CONTACT_EMAIL",
                fr = "Pour toute question sur cette politique ou sur tes données, contacte : $CONTACT_EMAIL",
            ),
        ),
    )

    val onlineVersionLabel: String = lang.pick(en = "View the online version", fr = "Voir la version en ligne")
    val onlineVersionHint: String = lang.pick(
        en = "Opens the online version of this policy in your browser, kept identical to this screen.",
        fr = "Ouvre la version en ligne de cette politique dans ton navigateur, tenue identique à cet écran.",
    )

    companion object {
        fun of(language: AppLanguage): PrivacyContent = PrivacyContent(language)

        const val CONTACT_EMAIL = "bizwadan@gmail.com"

        const val ONLINE_URL = "https://gist.github.com/dandmb/c7461e74a35140d79e657a19eb66566f"
    }
}
