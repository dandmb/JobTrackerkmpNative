package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Garde-fou (lecture du source) : les écrans dont la barre du haut est en `primary` règlent la couleur des icônes de la barre
 * d'état (thème sombre : `primary` est clair → icônes sombres). Défaut constaté sur émulateur : icônes blanches sur teal clair.
 */
class StatusBarIconsWiringTest {

    private fun read(vararg candidates: String) = candidates.map(::File).first { it.exists() }.readText()

    private val helper = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/theme/StatusBarIcons.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/theme/StatusBarIcons.kt",
    )
    private val list = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/joboffer/JobOfferListScreen.kt",
    )
    private val about = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/about/AboutScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/about/AboutScreen.kt",
    )

    @Test
    fun helper_setsLightStatusBarIconsOnlyInDarkThemeAndRestoresThePreviousState() {
        assertTrue(helper.contains("isAppearanceLightStatusBars = darkTheme"), "icônes sombres en thème sombre, claires en thème clair")
        assertTrue(helper.contains("isSystemInDarkTheme()"))
        assertTrue(helper.contains("onDispose { controller.isAppearanceLightStatusBars = previous }"))
    }

    @Test
    fun screensWithAPrimaryTopBar_useTheHelper() {
        assertTrue(list.contains("StatusBarIconsForPrimaryTopBar()"))
        assertTrue(about.contains("StatusBarIconsForPrimaryTopBar()"))
    }

    @Test
    fun topBars_stillUsePrimaryAsContainerColor_theAssumptionOfTheHelper() {
        // Si la couleur de la barre du haut change, la règle « thème sombre → icônes sombres » doit être revue.
        assertEquals(1, Regex("containerColor = MaterialTheme.colorScheme.primary").findAll(list).count())
        assertTrue(about.contains("containerColor = MaterialTheme.colorScheme.primary"))
    }
}
