package com.dmb.joblog.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Parité Android ↔ iOS : les libellés présents sur les DEUX plateformes ont exactement le même texte dans chaque langue
 * (un même mot ne doit pas diverger entre `strings.xml` et `Localizable.strings` — ce qui est déjà arrivé avec les formulaires).
 * Les clés propres à une plateforme (snackbar Android, dialogue et champs iOS…) sont volontairement hors comparaison.
 */
class LocalizationParityTest {

    private val androidOnly = setOf("app_name", "ok", "list_deleted_snackbar", "form_field_salary_min", "form_field_salary_max",
        "form_date_interview", "form_date_result")
    private val iosOnly = setOf("list_delete_confirm_title", "list_delete_confirm_message", "card_status_hint", "form_section_job",
        "form_section_salary", "form_salary_min_short", "form_salary_max_short", "form_interview_toggle", "form_result_toggle",
        "form_date_interview_short", "form_date_result_short")

    private fun normalized(map: Map<String, String>) = map.mapValues { StringResources.normalizePlaceholders(it.value) }

    @Test
    fun iosFiles_exist_andAreNotEmpty() {
        assertTrue(StringResources.iosEn.size > 40, "en.lproj/Localizable.strings introuvable ou vide")
        assertTrue(StringResources.iosFr.size > 40)
    }

    @Test
    fun keys_areEitherSharedByBothPlatforms_orDeclaredPlatformSpecific() {
        val android = StringResources.androidEn.keys
        val ios = StringResources.iosEn.keys

        assertEquals(emptySet(), (android - ios) - androidOnly, "clé Android absente d'iOS et non déclarée « Android seulement »")
        assertEquals(emptySet(), (ios - android) - iosOnly, "clé iOS absente d'Android et non déclarée « iOS seulement »")
        assertTrue((android intersect ios).size >= 40, "trop peu de clés partagées : ${(android intersect ios).size}")
    }

    @Test
    fun sharedKeys_haveTheSameEnglishText_onBothPlatforms() {
        val android = normalized(StringResources.androidEn)
        val ios = normalized(StringResources.iosEn)
        (android.keys intersect ios.keys).forEach { assertEquals(android.getValue(it), ios.getValue(it), "EN : « $it » diffère entre Android et iOS") }
    }

    @Test
    fun sharedKeys_haveTheSameFrenchText_onBothPlatforms() {
        val android = normalized(StringResources.androidFr)
        val ios = normalized(StringResources.iosFr)
        (android.keys intersect ios.keys).forEach { assertEquals(android.getValue(it), ios.getValue(it), "FR : « $it » diffère entre Android et iOS") }
    }

    @Test
    fun iosFiles_haveTheSameKeysInBothLanguages() {
        assertEquals(StringResources.iosEn.keys, StringResources.iosFr.keys)
    }

    @Test
    fun iosProject_declaresFrenchAsAKnownRegion_andTheBundleLocalizations() {
        val project = StringResources.repoFile("iosApp/iosApp.xcodeproj/project.pbxproj").readText()
        val plist = StringResources.repoFile("iosApp/iosApp/Info.plist").readText()

        assertTrue(Regex("knownRegions = \\(\\s*en,\\s*fr,\\s*Base,?\\s*\\)").containsMatchIn(project))
        assertTrue("<key>CFBundleDevelopmentRegion</key>" in plist && "<string>en</string>" in plist)
        assertTrue("<key>CFBundleLocalizations</key>" in plist && "<string>fr</string>" in plist)
    }
}
