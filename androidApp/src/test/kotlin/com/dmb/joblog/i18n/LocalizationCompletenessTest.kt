package com.dmb.joblog.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Chaque texte de l'interface Android existe en anglais (langue par défaut) ET en français, sans texte oublié dans la mauvaise langue. */
class LocalizationCompletenessTest {

    private val en = StringResources.androidEn
    private val fr = StringResources.androidFr
    private val accents = Regex("[àâçéèêëîïôûùüÿœ«»]", RegexOption.IGNORE_CASE)

    /** Textes identiques dans les deux langues par nature (nom propre, sigle, symboles). */
    private val sameInBothLanguages = setOf("app_name", "ok", "sort_az", "sort_za", "form_section_dates", "form_field_notes")

    @Test
    fun bothFiles_areNotEmpty() {
        assertTrue(en.size > 40, "strings.xml (en) anormalement court : ${en.size}")
        assertTrue(fr.size > 40)
    }

    @Test
    fun everyKey_existsInEnglishAndInFrench() {
        assertEquals(emptySet(), en.keys - fr.keys, "clés sans traduction française")
        assertEquals(emptySet(), fr.keys - en.keys, "clés françaises sans version anglaise (langue par défaut)")
    }

    @Test
    fun placeholders_areTheSameInBothLanguages() {
        fun args(value: String) = Regex("%\\d").findAll(StringResources.normalizePlaceholders(value)).map { it.value }.sorted().toList()
        en.forEach { (key, value) -> assertEquals(args(value), args(fr.getValue(key)), "arguments différents pour « $key »") }
    }

    @Test
    fun noValue_isBlank() {
        (en + fr.mapKeys { "fr:" + it.key }).forEach { (key, value) -> assertTrue(value.isNotBlank(), "valeur vide : $key") }
    }

    @Test
    fun englishFile_containsNoFrenchAccentOrGuillemet() {
        en.forEach { (key, value) -> assertFalse(accents.containsMatchIn(value), "texte français dans strings.xml (en) : $key = $value") }
    }

    @Test
    fun everyText_isTranslated_exceptTheKnownInvariants() {
        en.filterKeys { it !in sameInBothLanguages }.forEach { (key, value) ->
            val french = fr.getValue(key)
            // les libellés courts de statistiques peuvent coïncider (« %1$d attente » ≠ « %1$d pending » : non ; « Notes » : invariant listé)
            assertTrue(value != french, "« $key » identique en anglais et en français : $value")
        }
    }

    @Test
    fun frenchFile_usesTheApostropheEscapesAndKeepsItsAccents() {
        assertEquals("Candidatures", fr["list_title"])
        assertEquals("Applications", en["list_title"])
        assertEquals("Lien de l'annonce", fr["form_field_url"])
    }

    @Test
    fun statusVocabulary_matchesTheSharedOnboardingText() {
        // La 2e page de l'onboarding (sharedLogic) cite les statuts : mêmes mots que les libellés de statut des écrans.
        val onboardingEn = com.dmb.joblog.presentation.onboarding.OnboardingContent.of(AppLanguage.EN).pages[1].description
        val onboardingFr = com.dmb.joblog.presentation.onboarding.OnboardingContent.of(AppLanguage.FR).pages[1].description
        listOf("status_pending", "status_applied", "status_interview", "status_rejected", "status_accepted").forEach {
            assertTrue(en.getValue(it) in onboardingEn, "« ${en.getValue(it)} » absent de l'onboarding (EN)")
            assertTrue(fr.getValue(it) in onboardingFr, "« ${fr.getValue(it)} » absent de l'onboarding (FR)")
        }
    }

    @Test
    fun manifest_declaresTheSupportedLocales_englishAndFrench() {
        val locales = StringResources.androidFile("res/xml/locales_config.xml").readText()
        assertTrue("android:name=\"en\"" in locales && "android:name=\"fr\"" in locales)
        assertTrue("android:localeConfig=\"@xml/locales_config\"" in StringResources.androidFile("AndroidManifest.xml").readText())
    }
}
