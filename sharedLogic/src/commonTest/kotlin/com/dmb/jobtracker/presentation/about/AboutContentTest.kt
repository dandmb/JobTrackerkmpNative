package com.dmb.jobtracker.presentation.about

import com.dmb.jobtracker.testutil.jobOffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AboutContentTest {

    private val allText: String = buildString {
        AboutContent.sections.forEach { s -> append(s.title, ' ', s.intro.orEmpty(), ' ', s.bullets.joinToString(" "), ' ', s.note.orEmpty(), ' ') }
        append(AboutContent.CONTACT_INTRO, ' ', AboutContent.CONTACT_NOTE, ' ', AboutContent.DELETE_ALL_EXPLANATION, ' ')
        append(AboutContent.firstConfirmation.message, ' ', AboutContent.finalConfirmation.message)
    }

    private fun section(title: String) = AboutContent.sections.single { it.title == title }

    // ---------- fidélité au modèle de données ----------

    /** Noms des propriétés du modèle, lus dans le `toString()` d'une `data class` (pas de réflexion en commonTest). */
    private fun jobOfferFieldNames(): List<String> =
        Regex("""(\w+)=""").findAll(jobOffer().toString()).map { it.groupValues[1] }.toList()

    @Test
    fun jobOfferFields_areExactlyTheOnesTheTextWasWrittenFor() {
        // Si ce test échoue, un champ a été ajouté/retiré/renommé dans JobOffer : relire « Ce que l'app enregistre ».
        assertEquals(
            listOf("id", "title", "company", "url", "location", "source", "salaryRange",
                "appliedDate", "interviewDate", "resultDate", "status", "notes"),
            jobOfferFieldNames(),
        )
    }

    @Test
    fun storedDataSection_mentionsEveryUserEnteredField() {
        val text = (section("Ce que l'app enregistre").bullets).joinToString(" ").lowercase()
        val keywordPerField = mapOf(
            "title" to "titre", "company" to "entreprise", "url" to "lien", "location" to "localisation",
            "source" to "source", "salaryRange" to "salaire", "appliedDate" to "candidature",
            "interviewDate" to "entretien", "resultDate" to "résultat", "status" to "statut", "notes" to "notes",
        )
        // Tous les champs saisis (tous sauf l'id technique) sont couverts par un mot-clé.
        assertEquals(jobOfferFieldNames().filter { it != "id" }.toSet(), keywordPerField.keys)
        keywordPerField.forEach { (field, keyword) -> assertTrue(keyword in text, "« $keyword » (champ $field) absent du texte") }
    }

    @Test
    fun storedDataSection_mentionsTheTechnicalCreationDateAndTheWelcomeFlag() {
        val note = section("Ce que l'app enregistre").note.orEmpty()

        assertTrue("date et l'heure de création" in note)
        assertTrue("écran de bienvenue" in note)
    }

    // ---------- structure ----------

    @Test
    fun sections_areTheFourExpectedOnesInOrder() {
        assertEquals(
            listOf("Ce que l'app enregistre", "Où vivent tes données", "Ce que l'app ne fait pas", "Tes droits sur tes données"),
            AboutContent.sections.map { it.title },
        )
    }

    @Test
    fun everySection_hasATitleAndSomeBody() {
        AboutContent.sections.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.bullets.isNotEmpty() || !it.intro.isNullOrBlank(), "section « ${it.title} » vide")
        }
    }

    @Test
    fun noBulletOrText_isBlank() {
        AboutContent.sections.forEach { s ->
            s.bullets.forEach { assertTrue(it.isNotBlank()) }
            assertTrue(s.intro?.isNotBlank() ?: true)
            assertTrue(s.note?.isNotBlank() ?: true)
        }
    }

    @Test
    fun sectionTitles_areUnique() {
        assertEquals(AboutContent.sections.size, AboutContent.sections.map { it.title }.toSet().size)
    }

    // ---------- affirmations de confidentialité (vérifiées dans le code, cf. rapport) ----------

    @Test
    fun localStorageSection_saysDataStaysOnTheDeviceAndNothingIsSent() {
        val bullets = section("Où vivent tes données").bullets.joinToString(" ")

        assertTrue("sur ton appareil" in bullets)
        assertTrue("nulle part" in bullets)
        assertTrue("Aucun compte" in bullets)
    }

    @Test
    fun localStorageSection_disclosesTheAndroidFontDownloadAndTheSystemBackups() {
        val section = section("Où vivent tes données")

        assertTrue(section.bullets.any { "Android" in it && "police" in it && "jamais tes candidatures" in it })
        assertTrue("sauvegarde Google" in section.note.orEmpty() && "iCloud" in section.note.orEmpty())
    }

    @Test
    fun notDoingSection_coversTrackingAdsAndPermissions() {
        val bullets = section("Ce que l'app ne fait pas").bullets.joinToString(" ")

        assertTrue("analyse" in bullets)
        assertTrue("publicité" in bullets)
        assertTrue("autorisation" in bullets)
    }

    @Test
    fun rightsSection_pointsToEditingDeletingAndTheDeleteAllButton() {
        val bullets = section("Tes droits sur tes données").bullets.joinToString(" ")

        assertTrue("consulter" in bullets)
        assertTrue("modifier" in bullets)
        assertTrue("glisse-la" in bullets)
        assertTrue(AboutContent.DELETE_ALL_LABEL in bullets)
    }

    @Test
    fun rightsSection_saysExistingSystemBackupsAreNotErased() {
        assertTrue("n'est pas effacée" in section("Tes droits sur tes données").note.orEmpty())
    }

    @Test
    fun text_neverMakesAnAbsoluteClaimTheCodeCannotBackUp() {
        // Formulations volontairement proscrites : « jamais partagées » sans nuance, « 100 % sécurisé », conformité légale…
        val lower = allText.lowercase()
        listOf("rgpd", "100 %", "sécurisé", "chiffré", "conforme", "garantit", "anonym").forEach {
            assertFalse(it in lower, "affirmation non vérifiée : « $it »")
        }
    }

    // ---------- version ----------

    @Test
    fun versionLabel_withVersionAndBuild_isFormatted() {
        assertEquals("Version 1.0 (1)", AboutContent.versionLabel("1.0", "1"))
    }

    @Test
    fun versionLabel_withoutBuild_omitsTheParentheses() {
        assertEquals("Version 2.3.1", AboutContent.versionLabel("2.3.1", ""))
        assertEquals("Version 2.3.1", AboutContent.versionLabel("2.3.1", "  "))
    }

    @Test
    fun versionLabel_blankVersion_showsAPlaceholderInsteadOfNothing() {
        assertEquals("Version ? (7)", AboutContent.versionLabel("", "7"))
    }

    @Test
    fun versionLabel_trimsItsInputs() {
        assertEquals("Version 1.0 (5)", AboutContent.versionLabel(" 1.0 ", " 5 "))
    }

    @Test
    fun versionLabel_dependsOnItsInput_notAHardCodedValue() {
        assertNotEquals(AboutContent.versionLabel("1.0", "1"), AboutContent.versionLabel("1.1", "1"))
    }

    // ---------- suppression ----------

    @Test
    fun deleteAllTexts_areFilledAndDistinct() {
        val first = AboutContent.firstConfirmation
        val final = AboutContent.finalConfirmation

        listOf(first.title, first.message, first.confirmLabel, first.cancelLabel,
            final.title, final.message, final.confirmLabel, final.cancelLabel,
            AboutContent.DELETE_ALL_LABEL, AboutContent.DELETE_ALL_EXPLANATION,
            AboutContent.DELETE_ALL_SUCCESS_MESSAGE).forEach { assertTrue(it.isNotBlank()) }
        assertNotEquals(first.title, final.title)
        assertNotEquals(first.confirmLabel, final.confirmLabel)
    }

    @Test
    fun deleteAllTexts_stateThatTheActionIsIrreversible() {
        assertTrue("irréversible" in AboutContent.DELETE_ALL_EXPLANATION)
        assertTrue("récupérer" in AboutContent.firstConfirmation.message)
        assertTrue("définitivement" in AboutContent.finalConfirmation.message)
    }

    @Test
    fun deleteAllLabel_isTheExactWordingRequestedByTheProduct() {
        assertEquals("Supprimer toutes mes données", AboutContent.DELETE_ALL_LABEL)
    }

    @Test
    fun finalConfirmationButton_isClearlyDestructive() {
        assertEquals("Tout supprimer", AboutContent.finalConfirmation.confirmLabel)
        assertEquals("Annuler", AboutContent.finalConfirmation.cancelLabel)
    }

    // ---------- licences : volontairement absentes ----------

    @Test
    fun text_mentionsNoThirdPartyLicenseNorLibraryNorFont() {
        // Décision explicite du propriétaire : aucune licence de dépendance ou de police n'est affichée.
        val lower = allText.lowercase()
        listOf("licence", "license", "open source", "open-source", "apache", "mit ", "ofl", "jakarta", "kotlin", "jetpack",
            "room", "koin", "androidx", "rxswift", "nativecoroutines", "bibliothèque").forEach {
            assertFalse(it in lower, "mention d'un élément tiers dans le texte de l'écran : « $it »")
        }
    }

    // ---------- contact ----------

    @Test
    fun contact_emailAndSubject_areTheExpectedConstants() {
        assertEquals("bizwadan@gmail.com", AboutContent.CONTACT_EMAIL)
        assertEquals("JobTracker - Contact", AboutContent.CONTACT_SUBJECT)
        assertEquals("Nous contacter", AboutContent.CONTACT_LABEL)
    }

    @Test
    fun contactMailtoUri_isAMailtoWithThePrefilledSubject() {
        assertEquals("mailto:bizwadan@gmail.com?subject=JobTracker%20-%20Contact", AboutContent.contactMailtoUri())
    }

    @Test
    fun contactMailtoUri_startsWithMailtoAndContainsNoRawSpace() {
        val uri = AboutContent.contactMailtoUri()

        assertTrue(uri.startsWith("mailto:${AboutContent.CONTACT_EMAIL}"))
        assertFalse(' ' in uri)
    }

    @Test
    fun percentEncode_unreservedCharacters_areKept() {
        assertEquals("AZaz09-._~", AboutContent.percentEncode("AZaz09-._~"))
    }

    @Test
    fun percentEncode_spaceAndReservedCharacters_areEncoded() {
        assertEquals("a%20b", AboutContent.percentEncode("a b"))
        assertEquals("%26%3D%3F%23%25%2B%2F%3A%40", AboutContent.percentEncode("&=?#%+/:@"))
    }

    @Test
    fun percentEncode_accentedAndNonAsciiCharacters_areEncodedAsUtf8Bytes() {
        assertEquals("%C3%A9", AboutContent.percentEncode("é"))
        assertEquals("%E2%82%AC", AboutContent.percentEncode("€"))
        assertEquals("%F0%9F%99%82", AboutContent.percentEncode("🙂"))
    }

    @Test
    fun percentEncode_emptyText_isEmpty() {
        assertEquals("", AboutContent.percentEncode(""))
    }

    @Test
    fun contactTexts_areFilled_andTheFallbackMessageShowsTheAddress() {
        listOf(AboutContent.CONTACT_TITLE, AboutContent.CONTACT_INTRO, AboutContent.CONTACT_NOTE).forEach { assertTrue(it.isNotBlank()) }
        assertTrue(AboutContent.CONTACT_EMAIL in AboutContent.CONTACT_NO_MAIL_APP_MESSAGE)
    }

    @Test
    fun contactNote_saysNothingIsSentUntilTheUserSendsIt() {
        assertTrue("tant que tu ne l'envoies pas toi-même" in AboutContent.CONTACT_NOTE)
    }

    @Test
    fun screenTexts_areTheExpectedOnes() {
        assertEquals("À propos", AboutContent.SCREEN_TITLE)
        assertEquals("JobTracker", AboutContent.APP_NAME)
    }
}
