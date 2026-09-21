package com.dmb.joblog.presentation.about

import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.testutil.jobOffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AboutContentTest {

    private val fr = AboutContent.of(AppLanguage.FR)
    private val en = AboutContent.of(AppLanguage.EN)
    private val both = listOf(fr, en)

    private fun AboutContent.allText(): String = buildString {
        sections.forEach { s -> append(s.title, ' ', s.intro.orEmpty(), ' ', s.bullets.joinToString(" "), ' ', s.note.orEmpty(), ' ') }
        append(contactIntro, ' ', contactNote, ' ', contactNoMailAppMessage, ' ', deleteAllExplanation, ' ', deleteAllFailedMessage, ' ')
        append(firstConfirmation.message, ' ', finalConfirmation.message, ' ', tagline, ' ', screenTitle)
    }

    private fun AboutContent.section(index: Int) = sections[index]

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
    fun storedDataSection_mentionsEveryUserEnteredField_inEachLanguage() {
        val keywords = mapOf(
            AppLanguage.FR to mapOf("title" to "titre", "company" to "entreprise", "url" to "lien", "location" to "localisation",
                "source" to "source", "salaryRange" to "salaire", "appliedDate" to "candidature", "interviewDate" to "entretien",
                "resultDate" to "résultat", "status" to "statut", "notes" to "notes"),
            AppLanguage.EN to mapOf("title" to "title", "company" to "company", "url" to "link", "location" to "location",
                "source" to "source", "salaryRange" to "salary", "appliedDate" to "application", "interviewDate" to "interview",
                "resultDate" to "result", "status" to "status", "notes" to "notes"),
        )
        AppLanguage.entries.forEach { language ->
            val text = AboutContent.of(language).section(0).bullets.joinToString(" ").lowercase()
            val map = keywords.getValue(language)
            // Tous les champs saisis (tous sauf l'id technique) sont couverts par un mot-clé.
            assertEquals(jobOfferFieldNames().filter { it != "id" }.toSet(), map.keys)
            map.forEach { (field, keyword) -> assertTrue(keyword in text, "« $keyword » (champ $field) absent du texte $language") }
        }
    }

    @Test
    fun storedDataSection_mentionsTheTechnicalCreationDateAndTheWelcomeFlag() {
        assertTrue("date et l'heure de création" in fr.section(0).note.orEmpty())
        assertTrue("écran de bienvenue" in fr.section(0).note.orEmpty())
        assertTrue("date and time" in en.section(0).note.orEmpty())
        assertTrue("welcome screen" in en.section(0).note.orEmpty())
    }

    // ---------- structure ----------

    @Test
    fun sections_areTheFourExpectedOnesInOrder() {
        assertEquals(
            listOf("Ce que l'app enregistre", "Où vivent tes données", "Ce que l'app ne fait pas", "Tes droits sur tes données"),
            fr.sections.map { it.title },
        )
        assertEquals(
            listOf("What the app stores", "Where your data lives", "What the app does not do", "Your rights over your data"),
            en.sections.map { it.title },
        )
    }

    @Test
    fun bothLanguages_haveTheSameStructure() {
        assertEquals(fr.sections.size, en.sections.size)
        fr.sections.zip(en.sections).forEach { (f, e) ->
            assertEquals(f.bullets.size, e.bullets.size, "nombre de puces différent : ${f.title}")
            assertEquals(f.intro == null, e.intro == null)
            assertEquals(f.note == null, e.note == null)
        }
    }

    @Test
    fun everySection_hasATitleAndSomeBody() {
        both.forEach { c ->
            c.sections.forEach {
                assertTrue(it.title.isNotBlank())
                assertTrue(it.bullets.isNotEmpty() || !it.intro.isNullOrBlank(), "section « ${it.title} » vide")
            }
        }
    }

    @Test
    fun noBulletOrText_isBlank() {
        both.forEach { c ->
            c.sections.forEach { s ->
                s.bullets.forEach { assertTrue(it.isNotBlank()) }
                assertTrue(s.intro?.isNotBlank() ?: true)
                assertTrue(s.note?.isNotBlank() ?: true)
            }
        }
    }

    @Test
    fun sectionTitles_areUnique() {
        both.forEach { assertEquals(it.sections.size, it.sections.map { s -> s.title }.toSet().size) }
    }

    // ---------- traduction complète ----------

    @Test
    fun everyText_isTranslated_noStringIsIdenticalInBothLanguages() {
        fr.sections.zip(en.sections).forEach { (f, e) ->
            assertNotEquals(f.title, e.title)
            f.bullets.zip(e.bullets).forEach { (fb, eb) -> assertNotEquals(fb, eb, "puce non traduite : $fb") }
            if (f.intro != null) assertNotEquals(f.intro, e.intro)
            if (f.note != null) assertNotEquals(f.note, e.note)
        }
        listOf(
            fr.tagline to en.tagline, fr.backLabel to en.backLabel, fr.deleteAllLabel to en.deleteAllLabel,
            fr.deleteAllExplanation to en.deleteAllExplanation, fr.deleteAllSuccessMessage to en.deleteAllSuccessMessage,
            fr.deleteAllInProgressLabel to en.deleteAllInProgressLabel, fr.deleteAllFailedMessage to en.deleteAllFailedMessage,
            fr.firstConfirmation.title to en.firstConfirmation.title, fr.firstConfirmation.message to en.firstConfirmation.message,
            fr.firstConfirmation.confirmLabel to en.firstConfirmation.confirmLabel, fr.finalConfirmation.title to en.finalConfirmation.title,
            fr.finalConfirmation.message to en.finalConfirmation.message, fr.finalConfirmation.confirmLabel to en.finalConfirmation.confirmLabel,
            fr.contactTitle to en.contactTitle, fr.contactIntro to en.contactIntro, fr.contactNote to en.contactNote,
            fr.contactNoMailAppMessage to en.contactNoMailAppMessage, fr.screenTitle to en.screenTitle,
        ).forEach { (f, e) -> assertNotEquals(f, e, "texte non traduit : $f") }
    }

    @Test
    fun englishTexts_containNoFrenchAccent() {
        val accents = Regex("[àâçéèêëîïôûùüÿœ]", RegexOption.IGNORE_CASE)
        assertFalse(accents.containsMatchIn(en.allText()), "texte français oublié dans la version anglaise")
    }

    @Test
    fun frenchTexts_stillUseTheirAccents() {
        assertTrue("À propos" == fr.screenTitle && "Où vivent tes données" == fr.sections[1].title)
    }

    // ---------- affirmations de confidentialité (vérifiées dans le code, cf. rapport) ----------

    @Test
    fun localStorageSection_saysDataStaysOnTheDeviceAndNothingIsSent() {
        val f = fr.section(1).bullets.joinToString(" ")
        val e = en.section(1).bullets.joinToString(" ")

        assertTrue("sur ton appareil" in f && "nulle part" in f && "Aucun compte" in f)
        assertTrue("on your device" in e && "not sent anywhere" in e && "No account" in e)
    }

    @Test
    fun localStorageSection_disclosesTheAndroidFontDownloadAndTheSystemBackups() {
        assertTrue(fr.section(1).bullets.any { "Android" in it && "police" in it && "jamais tes candidatures" in it })
        assertTrue("sauvegarde Google" in fr.section(1).note.orEmpty() && "iCloud" in fr.section(1).note.orEmpty())
        assertTrue(en.section(1).bullets.any { "Android" in it && "font" in it && "never your applications" in it })
        assertTrue("Google backup" in en.section(1).note.orEmpty() && "iCloud" in en.section(1).note.orEmpty())
    }

    @Test
    fun notDoingSection_coversTrackingAdsAndPermissions() {
        val f = fr.section(2).bullets.joinToString(" ")
        val e = en.section(2).bullets.joinToString(" ")

        assertTrue("analyse" in f && "publicité" in f && "autorisation" in f)
        assertTrue("analytics" in e && "ads" in e && "permission" in e)
    }

    @Test
    fun rightsSection_pointsToEditingDeletingAndTheDeleteAllButton() {
        val f = fr.section(3).bullets.joinToString(" ")
        val e = en.section(3).bullets.joinToString(" ")

        assertTrue("consulter" in f && "modifier" in f && "glisse-la" in f && fr.deleteAllLabel in f)
        assertTrue("view" in e && "edit" in e && "swipe" in e && en.deleteAllLabel in e)
    }

    @Test
    fun rightsSection_saysExistingSystemBackupsAreNotErased() {
        assertTrue("n'est pas effacée" in fr.section(3).note.orEmpty())
        assertTrue("is not erased" in en.section(3).note.orEmpty())
    }

    @Test
    fun text_neverMakesAnAbsoluteClaimTheCodeCannotBackUp() {
        // Formulations volontairement proscrites : « 100 % sécurisé », conformité légale…
        both.forEach { c ->
            val lower = c.allText().lowercase()
            listOf("rgpd", "gdpr", "100 %", "100%", "sécurisé", "secure", "chiffré", "encrypted", "conforme", "compliant", "garantit", "guarantee", "anonym")
                .forEach { assertFalse(it in lower, "affirmation non vérifiée : « $it »") }
        }
    }

    // ---------- version ----------

    @Test
    fun versionLabel_isFormattedTheSameInBothLanguages() {
        both.forEach {
            assertEquals("Version 1.0 (1)", it.versionLabel("1.0", "1"))
            assertEquals("Version 2.3.1", it.versionLabel("2.3.1", ""))
            assertEquals("Version 2.3.1", it.versionLabel("2.3.1", "  "))
            assertEquals("Version ? (7)", it.versionLabel("", "7"))
            assertEquals("Version 1.0 (5)", it.versionLabel(" 1.0 ", " 5 "))
        }
    }

    @Test
    fun versionLabel_dependsOnItsInput_notAHardCodedValue() {
        assertNotEquals(fr.versionLabel("1.0", "1"), fr.versionLabel("1.1", "1"))
    }

    // ---------- suppression ----------

    @Test
    fun deleteAllTexts_areFilled_andConfirmationsAreDistinct() {
        both.forEach { c ->
            listOf(c.firstConfirmation.title, c.firstConfirmation.message, c.firstConfirmation.confirmLabel, c.firstConfirmation.cancelLabel,
                c.finalConfirmation.title, c.finalConfirmation.message, c.finalConfirmation.confirmLabel, c.finalConfirmation.cancelLabel,
                c.deleteAllLabel, c.deleteAllExplanation, c.deleteAllSuccessMessage, c.deleteAllFailedMessage).forEach { assertTrue(it.isNotBlank()) }
            assertNotEquals(c.firstConfirmation.title, c.finalConfirmation.title)
            assertNotEquals(c.firstConfirmation.confirmLabel, c.finalConfirmation.confirmLabel)
        }
    }

    @Test
    fun deleteAllTexts_stateThatTheActionIsIrreversible() {
        assertTrue("irréversible" in fr.deleteAllExplanation && "récupérer" in fr.firstConfirmation.message && "définitivement" in fr.finalConfirmation.message)
        assertTrue("cannot be undone" in en.deleteAllExplanation && "recover" in en.firstConfirmation.message && "permanently" in en.finalConfirmation.message)
    }

    @Test
    fun deleteAllLabel_isTheExactWordingOfTheProduct() {
        assertEquals("Supprimer toutes mes données", fr.deleteAllLabel)
        assertEquals("Delete all my data", en.deleteAllLabel)
    }

    @Test
    fun finalConfirmationButtons_areClearlyDestructiveAndCancel() {
        assertEquals("Tout supprimer" to "Annuler", fr.finalConfirmation.confirmLabel to fr.finalConfirmation.cancelLabel)
        assertEquals("Delete everything" to "Cancel", en.finalConfirmation.confirmLabel to en.finalConfirmation.cancelLabel)
    }

    // ---------- licences : volontairement absentes ----------

    @Test
    fun text_mentionsNoThirdPartyLicenseNorLibraryNorFont() {
        // Décision explicite du propriétaire : aucune licence de dépendance ou de police n'est affichée.
        both.forEach { c ->
            val lower = c.allText().lowercase()
            listOf("licence", "license", "open source", "open-source", "apache", "mit ", "ofl", "jakarta", "kotlin", "jetpack",
                "room", "koin", "androidx", "rxswift", "nativecoroutines", "bibliothèque", "librar")
                .forEach { assertFalse(it in lower, "mention d'un élément tiers dans le texte de l'écran : « $it »") }
        }
    }

    // ---------- contact (indépendant de la langue) ----------

    @Test
    fun contact_emailAndSubject_areTheExpectedConstants() {
        assertEquals("bizwadan@gmail.com", AboutContent.CONTACT_EMAIL)
        assertEquals("JobLog - Contact", AboutContent.CONTACT_SUBJECT)
        assertEquals("Nous contacter", fr.contactLabel)
        assertEquals("Contact us", en.contactLabel)
    }

    @Test
    fun contactMailtoUri_isAMailtoWithThePrefilledSubject() {
        assertEquals("mailto:bizwadan@gmail.com?subject=JobLog%20-%20Contact", AboutContent.contactMailtoUri())
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
        both.forEach {
            listOf(it.contactTitle, it.contactIntro, it.contactNote).forEach { t -> assertTrue(t.isNotBlank()) }
            assertTrue(AboutContent.CONTACT_EMAIL in it.contactNoMailAppMessage)
        }
    }

    @Test
    fun contactNote_saysNothingIsSentUntilTheUserSendsIt() {
        assertTrue("tant que tu ne l'envoies pas toi-même" in fr.contactNote)
        assertTrue("until you send it yourself" in en.contactNote)
    }

    @Test
    fun screenTexts_areTheExpectedOnes() {
        assertEquals("À propos", fr.screenTitle)
        assertEquals("About", en.screenTitle)
        assertEquals("JobLog", AboutContent.APP_NAME)
    }

    @Test
    fun theAppName_neverMentionsTheOldName() {
        both.forEach { assertFalse("JobTracker" in it.allText()) }
        assertFalse("JobTracker" in AboutContent.CONTACT_SUBJECT)
    }
}
