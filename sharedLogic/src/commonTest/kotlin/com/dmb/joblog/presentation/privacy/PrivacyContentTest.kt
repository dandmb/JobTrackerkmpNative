package com.dmb.joblog.presentation.privacy

import com.dmb.joblog.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verrouille le texte français **verbatim** (fourni et validé par le propriétaire le 2026-09-27, identique au Gist
 * hébergé en ligne : `PrivacyContent.ONLINE_URL`). Si ce test échoue, quelqu'un a reformulé le texte : NE PAS corriger le
 * test pour le faire passer, restaurer le texte original ou obtenir un nouveau texte validé.
 */
class PrivacyContentTest {

    private val fr = PrivacyContent.of(AppLanguage.FR)
    private val en = PrivacyContent.of(AppLanguage.EN)

    @Test
    fun frenchText_isExactlyTheApprovedVerbatimText() {
        assertEquals("Politique de confidentialité — JobLog", fr.documentTitle)
        assertEquals("Dernière mise à jour : 27 septembre 2026", fr.lastUpdatedLabel)
        assertEquals(
            "JobLog est une application de suivi de candidatures développée par Dan Bizwa. Cette politique explique quelles données l'application traite et comment.",
            fr.introText,
        )

        assertEquals(8, fr.sections.size)

        val summary = fr.sections[0]
        assertEquals("1. Résumé", summary.title)
        assertEquals(
            "JobLog fonctionne entièrement sur ton appareil. L'application ne collecte, ne transmet et ne partage aucune donnée avec ses développeurs, avec des tiers ou avec un serveur : elle n'en a pas.",
            summary.intro,
        )

        val dataRecorded = fr.sections[1]
        assertEquals("2. Données enregistrées", dataRecorded.title)
        assertEquals("L'application enregistre uniquement ce que tu saisis toi-même dans une candidature :", dataRecorded.intro)
        assertEquals(
            listOf(
                "le titre du poste et l'entreprise ;",
                "la localisation, la source (LinkedIn, cooptation…) et le lien de l'annonce ;",
                "la fourchette de salaire ;",
                "les dates de candidature, d'entretien et de résultat ;",
                "le statut de la candidature ;",
                "tes notes.",
            ),
            dataRecorded.bullets,
        )
        assertEquals(
            "L'application ajoute automatiquement la date et l'heure de création de chaque candidature, pour ordonner la liste, et retient si tu as déjà vu l'écran de bienvenue. Elle ne te demande ni nom, ni adresse e-mail, ni création de compte.",
            dataRecorded.note,
        )

        val whereStored = fr.sections[2]
        assertEquals("3. Où sont stockées tes données", whereStored.title)
        assertEquals(
            "Toutes tes données sont stockées dans une base de données locale, sur ton appareil. L'application ne contient aucun code qui communique avec un serveur : tes candidatures ne sont envoyées nulle part, ni à Dan Bizwa, ni à un tiers.",
            whereStored.intro,
        )
        assertEquals(
            "Exception, sur Android uniquement : la police d'écriture de l'application est fournie par les services Google Play, qui la téléchargent lors du premier lancement. Cette requête réseau ne concerne que la police d'écriture ; elle ne transmet aucune de tes candidatures ni aucune donnée personnelle.\n\n" +
                "Sauvegardes système : selon les réglages de ton appareil, le système d'exploitation (sauvegarde Google sur Android, iCloud ou sauvegarde locale sur iOS) peut inclure les données de l'application dans une sauvegarde générale de ton téléphone. Ce mécanisme est géré par le système d'exploitation, pas par JobLog, et échappe au contrôle de l'application.",
            whereStored.note,
        )

        val whatItDoesNot = fr.sections[3]
        assertEquals("4. Ce que l'application ne fait pas", whatItDoesNot.title)
        assertEquals(
            listOf(
                "Aucun outil d'analyse d'usage ou de statistiques n'est intégré.",
                "Aucune publicité.",
                "Aucun compte utilisateur, aucun identifiant publicitaire.",
                "Aucune autorisation sensible n'est demandée (localisation, contacts, photos, micro…).",
                "Aucun partage de données avec des tiers.",
            ),
            whatItDoesNot.bullets,
        )

        val yourRights = fr.sections[4]
        assertEquals("5. Tes droits sur tes données", yourRights.title)
        assertEquals("Tes données t'appartiennent et restent sous ton contrôle direct, dans l'application :", yourRights.intro)
        assertEquals(
            listOf(
                "Consultation et modification : accessibles à tout moment depuis la liste de tes candidatures.",
                "Suppression individuelle : chaque candidature peut être supprimée séparément.",
                "Suppression totale : l'écran « À propos » propose une option pour supprimer immédiatement et intégralement toutes tes données de l'application.",
            ),
            yourRights.bullets,
        )
        assertEquals(
            "Comme aucune donnée n'est stockée ailleurs que sur ton appareil, ces actions suffisent à effacer complètement tes informations, à l'exception d'une sauvegarde système déjà réalisée avant la suppression (voir section 3).",
            yourRights.note,
        )

        val minors = fr.sections[5]
        assertEquals("6. Mineurs", minors.title)
        assertEquals(
            "JobLog ne collecte aucune donnée personnelle identifiable et ne cible pas spécifiquement les mineurs. L'application ne demande aucune information permettant d'identifier l'âge de l'utilisateur.",
            minors.intro,
        )

        val changes = fr.sections[6]
        assertEquals("7. Modifications de cette politique", changes.title)
        assertEquals(
            "Cette politique peut être mise à jour si l'application évolue (par exemple, si une nouvelle fonctionnalité impliquait un traitement de données différent). La date de dernière mise à jour figure en haut de ce document. Toute évolution significative sera reflétée ici avant d'être publiée dans une nouvelle version de l'application.",
            changes.intro,
        )

        val contact = fr.sections[7]
        assertEquals("8. Contact", contact.title)
        assertEquals("Pour toute question sur cette politique ou sur tes données, contacte : bizwadan@gmail.com", contact.intro)
    }

    @Test
    fun onlineUrl_isTheGistGivenByTheOwner() {
        assertEquals("https://gist.github.com/dandmb/c7461e74a35140d79e657a19eb66566f", PrivacyContent.ONLINE_URL)
    }

    @Test
    fun contactEmail_matchesTheOneUsedElsewhereInTheApp() {
        // Même adresse que AboutContent.CONTACT_EMAIL (pas de duplication de constante volontaire : modules différents,
        // mais la valeur doit rester identique si l'un des deux change).
        assertEquals("bizwadan@gmail.com", PrivacyContent.CONTACT_EMAIL)
        assertTrue(fr.sections.last().intro!!.contains(PrivacyContent.CONTACT_EMAIL))
    }

    @Test
    fun englishText_existsForEverySection_andDiffersFromFrench() {
        assertEquals(8, en.sections.size)
        // « 8. Contact » est identique dans les deux langues (mot anglais déjà utilisé tel quel en français, comme
        // ailleurs dans le projet : app_name, ok…) : seul ce titre est dispensé de différer.
        val sameInBothLanguages = setOf("8. Contact")
        en.sections.zip(fr.sections).forEach { (enSection, frSection) ->
            assertTrue(enSection.title.isNotBlank())
            if (enSection.title !in sameInBothLanguages) {
                assertTrue(enSection.title != frSection.title, "« ${enSection.title} » devrait différer de « ${frSection.title} » (traduction manquante ?)")
            }
        }
        assertTrue(en.introText != fr.introText)
        assertTrue(en.documentTitle != fr.documentTitle)
    }

    @Test
    fun everySection_hasANonBlankTitle_inBothLanguages() {
        (fr.sections + en.sections).forEach { assertTrue(it.title.isNotBlank()) }
    }
}
