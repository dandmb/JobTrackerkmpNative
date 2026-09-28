package com.dmb.joblog.data.local

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Le dépôt d'onboarding contre un `Settings` en mémoire (MapSettings) : aucune dépendance à SharedPreferences / NSUserDefaults. */
class OnboardingRepositoryImplTest {

    private val settings = MapSettings()
    private val repository = OnboardingRepositoryImpl(settings)

    @Test
    fun hasCompletedOnboarding_freshInstall_isFalse() {
        assertFalse(repository.hasCompletedOnboarding())
    }

    @Test
    fun setOnboardingCompleted_thenHasCompleted_isTrue() {
        repository.setOnboardingCompleted()

        assertTrue(repository.hasCompletedOnboarding())
    }

    @Test
    fun setOnboardingCompleted_persistsUnderTheDocumentedKey() {
        repository.setOnboardingCompleted()

        assertEquals("onboarding_completed", ONBOARDING_COMPLETED_KEY)
        assertTrue(settings.getBoolean("onboarding_completed", false))
    }

    @Test
    fun hasCompletedOnboarding_readsTheDocumentedKey() {
        settings.putBoolean("onboarding_completed", true)

        assertTrue(repository.hasCompletedOnboarding())
    }

    @Test
    fun hasCompletedOnboarding_keyExplicitlyFalse_isFalse() {
        settings.putBoolean("onboarding_completed", false)

        assertFalse(repository.hasCompletedOnboarding())
    }

    @Test
    fun setOnboardingCompleted_calledTwice_staysTrue() {
        repository.setOnboardingCompleted()
        repository.setOnboardingCompleted()

        assertTrue(repository.hasCompletedOnboarding())
    }

    @Test
    fun setOnboardingCompleted_isVisibleToANewRepositoryOverTheSameSettings() {
        // Simule un relancement de l'app : nouvelle instance, mêmes préférences persistées.
        repository.setOnboardingCompleted()

        assertTrue(OnboardingRepositoryImpl(settings).hasCompletedOnboarding())
    }

    @Test
    fun hasCompletedOnboarding_otherSettingsInstance_isNotAffected() {
        repository.setOnboardingCompleted()

        assertFalse(OnboardingRepositoryImpl(MapSettings()).hasCompletedOnboarding())
    }

    @Test
    fun setOnboardingCompleted_leavesUnrelatedKeysUntouched() {
        settings.putString("autre_cle", "valeur")

        repository.setOnboardingCompleted()

        assertEquals("valeur", settings.getString("autre_cle", ""))
        assertEquals(setOf("autre_cle", "onboarding_completed"), settings.keys)
    }

    @Test
    fun hasCompletedOnboarding_unrelatedKeysOnly_isFalse() {
        settings.putBoolean("autre_drapeau", true)

        assertFalse(repository.hasCompletedOnboarding())
    }
}
