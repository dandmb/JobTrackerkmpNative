package com.dmb.jobtracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Garde-fous de câblage (lecture du source, comme `FormSheetUsesSharedLogicTest`) : ces règles ne sont pas exécutables en
 * test unitaire (Activity, splash système), mais leur violation casse l'app de façon silencieuse ou tardive.
 */
class SplashAndOnboardingWiringTest {

    private fun read(vararg candidates: String): String = candidates.map(::File).first { it.exists() }.readText()

    private val mainActivity = read(
        "src/main/kotlin/com/dmb/jobtracker/MainActivity.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/MainActivity.kt",
    )
    private val appRoot = read(
        "src/main/kotlin/com/dmb/jobtracker/ui/AppRoot.kt",
        "androidApp/src/main/kotlin/com/dmb/jobtracker/ui/AppRoot.kt",
    )
    private val manifest = read("src/main/AndroidManifest.xml", "androidApp/src/main/AndroidManifest.xml")
    private val themes = read("src/main/res/values/themes.xml", "androidApp/src/main/res/values/themes.xml")

    @Test
    fun installSplashScreen_isCalledBeforeSuperOnCreate() {
        val install = mainActivity.indexOf("installSplashScreen()")
        val superOnCreate = mainActivity.indexOf("super.onCreate(")

        assertTrue(install >= 0, "installSplashScreen() absent de MainActivity")
        assertTrue(install < superOnCreate, "installSplashScreen() doit être appelé AVANT super.onCreate() (contrainte de l'API)")
    }

    @Test
    fun splashGating_usesTheSharedRuleWithTheListLoadingState() {
        assertTrue(mainActivity.contains("SplashGating.shouldKeepSplash("))
        assertTrue(mainActivity.contains("jobOfferListViewModel.state.value.isLoading"))
    }

    @Test
    fun listViewModel_isInstantiatedOnceAtTheRootAndPassedDown() {
        // Une seule injection du ViewModel de la liste, au niveau de l'activité (pas de koinInject dans l'écran).
        assertEquals(1, Regex("""JobOfferListViewModel by inject\(\)""").findAll(mainActivity).count())
        assertTrue(mainActivity.contains("AppRoot(jobOfferListViewModel, onboardingViewModel)"))
        assertTrue(appRoot.contains("JobOfferListScreen(viewModel = jobOfferListViewModel,"), "l'écran de liste doit recevoir l'instance déjà chargée")
    }

    @Test
    fun onboarding_isCompletedThenNavigatesWithoutRestart() {
        assertTrue(appRoot.contains("onboardingViewModel.completeOnboarding()"))
        assertTrue(appRoot.contains("showOnboarding = false"))
        assertTrue(appRoot.contains("hasCompletedOnboarding()"))
    }

    @Test
    fun activityTheme_isTheSplashStartingTheme() {
        assertTrue(manifest.contains("@style/Theme.App.Starting"))
        assertTrue(themes.contains("parent=\"Theme.SplashScreen\""))
        assertTrue(themes.contains("postSplashScreenTheme"))
    }

    @Test
    fun splashBackground_isTheBrandTeal() {
        val colors = read("src/main/res/values/colors.xml", "androidApp/src/main/res/values/colors.xml")

        assertTrue(colors.contains("#0D6E68"), "le fond du splash doit être le teal primaire de la marque")
        assertTrue(themes.contains("windowSplashScreenBackground\">@color/splash_background"))
    }

    @Test
    fun splashIcon_isAVectorDrawableNotARaster() {
        val icon = read("src/main/res/drawable/ic_splash_logo.xml", "androidApp/src/main/res/drawable/ic_splash_logo.xml")

        assertTrue(icon.contains("<vector"))
        assertFalse(File("src/main/res/drawable/ic_splash_logo.png").exists())
    }
}
