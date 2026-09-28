package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatusBarIconsWiringTest {

    private fun read(vararg candidates: String) = candidates.map(::File).first { it.exists() }.readText()

    private val helper = read(
        "src/main/kotlin/com/dmb/joblog/ui/theme/StatusBarIcons.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/theme/StatusBarIcons.kt",
    )
    private val list = read(
        "src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt",
    )
    private val about = read(
        "src/main/kotlin/com/dmb/joblog/ui/about/AboutScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/about/AboutScreen.kt",
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
        assertEquals(1, Regex("containerColor = MaterialTheme.colorScheme.primary").findAll(list).count())
        assertTrue(about.contains("containerColor = MaterialTheme.colorScheme.primary"))
    }
}
