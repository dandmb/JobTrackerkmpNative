package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fous de l'écran « Politique de confidentialité » (lecture du source). Le texte VERBATIM lui-même est verrouillé
 * dans `PrivacyContentTest` (sharedLogic) : ici on vérifie seulement que l'écran ne fait que le mettre en forme.
 */
class PrivacyScreenWiringTest {

    private fun read(vararg candidates: String): String = candidates.map(::File).first { it.exists() }.readText()

    private val privacyScreen = read(
        "src/main/kotlin/com/dmb/joblog/ui/privacy/PrivacyScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/privacy/PrivacyScreen.kt",
    )

    @Test
    fun privacyScreen_takesItsTextFromTheSharedContent_andDoesNotHardCodeIt() {
        assertTrue(privacyScreen.contains("PrivacyContent"))
        assertTrue(privacyScreen.contains("content.sections"))
        assertTrue(privacyScreen.contains("content.documentTitle"))
        assertTrue(privacyScreen.contains("content.introText"))
        // Aucun morceau du texte français verbatim ne doit être recopié dans l'écran (seul PrivacyContent.kt le porte).
        assertFalse(privacyScreen.contains("Résumé"))
        assertFalse(privacyScreen.contains("JobTracker"))
    }

    @Test
    fun privacyScreen_linksToTheOnlineVersion_keptIdenticalByRequirement() {
        assertTrue(privacyScreen.contains("PrivacyContent.ONLINE_URL"))
        assertTrue(privacyScreen.contains("content.onlineVersionLabel"))
        assertTrue(privacyScreen.contains("content.onlineVersionHint"), "contentDescription approprié attendu")
        assertFalse(privacyScreen.contains("gist.github.com"), "l'URL doit venir de PrivacyContent.ONLINE_URL, pas être codée en dur dans l'écran")
    }

    @Test
    fun privacyScreen_usesTheStandardTheme_notExpressive() {
        listOf("MaterialExpressiveTheme", "MotionScheme.expressive", "LargeFlexibleTopAppBar").forEach {
            assertFalse(privacyScreen.contains(it), "« $it » ne doit pas apparaître dans l'écran Confidentialité")
        }
    }

    @Test
    fun privacyScreen_hasABackButton() {
        assertTrue(privacyScreen.contains("onBack"))
        assertTrue(privacyScreen.contains("content.backLabel"))
    }
}
