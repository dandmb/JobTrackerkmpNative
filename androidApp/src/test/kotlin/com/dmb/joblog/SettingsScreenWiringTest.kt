package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsScreenWiringTest {

    private fun read(vararg candidates: String): String = candidates.map(::File).first { it.exists() }.readText()

    private val settingsScreen = read(
        "src/main/kotlin/com/dmb/joblog/ui/settings/SettingsScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/settings/SettingsScreen.kt",
    )

    @Test
    fun settingsScreen_takesItsTextFromTheSharedContent() {
        assertTrue(settingsScreen.contains("SettingsContent"))
        assertTrue(settingsScreen.contains("content.aboutRowLabel"))
        assertTrue(settingsScreen.contains("content.privacyRowLabel"))
        assertFalse(settingsScreen.contains("\"Settings\"") || settingsScreen.contains("\"Réglages\""), "libellé codé en dur")
    }

    @Test
    fun settingsScreen_hasExactlyTwoEntries_aboutAndPrivacy() {
        assertTrue(settingsScreen.contains("onOpenAbout"))
        assertTrue(settingsScreen.contains("onOpenPrivacy"))
        assertTrue(settingsScreen.contains("SettingsRow("))
    }

    @Test
    fun settingsScreen_usesTheStandardTheme_notExpressive() {
        listOf("MaterialExpressiveTheme", "MotionScheme.expressive", "LargeFlexibleTopAppBar").forEach {
            assertFalse(settingsScreen.contains(it), "« $it » ne doit pas apparaître dans l'écran Réglages")
        }
    }

    @Test
    fun settingsScreen_hasABackButton() {
        assertTrue(settingsScreen.contains("onBack"))
        assertTrue(settingsScreen.contains("content.backLabel"))
    }
}
