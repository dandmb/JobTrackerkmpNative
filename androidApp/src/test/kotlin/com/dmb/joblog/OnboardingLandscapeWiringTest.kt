package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class OnboardingLandscapeWiringTest {

    private val source = listOf(
        "src/main/kotlin/com/dmb/joblog/ui/onboarding/OnboardingScreen.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/ui/onboarding/OnboardingScreen.kt",
    ).map(::File).first { it.exists() }.readText()

    @Test
    fun landscape_hasItsOwnLayout_decidedByTheAvailableSpace() {
        assertTrue(source.contains("BoxWithConstraints"), "l'orientation se déduit de la place disponible (écran partagé compris)")
        assertTrue(source.contains("if (maxWidth > maxHeight)"))
        assertTrue(source.contains("OnboardingLandscape("))
    }

    @Test
    fun landscape_givesTheIllustrationTheFullHeight_andPutsTextAndActionsBesideIt() {
        val landscape = source.substringAfter("private fun OnboardingLandscape(").substringBefore("\n@Composable")
        assertTrue(landscape.contains("Row("), "illustration et texte côte à côte")
        assertTrue(landscape.contains("HorizontalPager(") && landscape.contains("OnboardingImage("), "l'illustration défile à gauche")
        assertTrue(landscape.contains("pages[current].title") && landscape.contains("pages[current].description"))
        assertTrue(landscape.contains("PageIndicators(") && landscape.contains("content.primaryButtonLabel(current)"))
        assertTrue(landscape.contains("content.skipLabel"))
    }
}
