package com.dmb.joblog.presentation.onboarding

import com.dmb.joblog.testutil.FakeOnboardingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingViewModelTest {

    private val repository = FakeOnboardingRepository()
    private val viewModel = OnboardingViewModel(repository)

    @Test
    fun hasCompletedOnboarding_firstLaunch_isFalse() {
        assertFalse(viewModel.hasCompletedOnboarding())
    }

    @Test
    fun hasCompletedOnboarding_alreadyCompleted_isTrue() {
        repository.completed = true

        assertTrue(viewModel.hasCompletedOnboarding())
    }

    @Test
    fun hasCompletedOnboarding_delegatesToTheRepository() {
        viewModel.hasCompletedOnboarding()

        assertEquals(1, repository.hasCompletedCalls)
    }

    @Test
    fun completeOnboarding_marksTheOnboardingAsCompletedInTheRepository() {
        viewModel.completeOnboarding()

        assertEquals(1, repository.setCompletedCalls)
        assertTrue(repository.completed)
    }

    @Test
    fun completeOnboarding_thenHasCompleted_isTrue() {
        viewModel.completeOnboarding()

        assertTrue(viewModel.hasCompletedOnboarding())
    }

    @Test
    fun completeOnboarding_beforeCompleting_hasNoEffectOnReading() {
        assertFalse(viewModel.hasCompletedOnboarding())
        assertEquals(0, repository.setCompletedCalls)
    }

    @Test
    fun completeOnboarding_calledTwice_staysCompleted() {
        viewModel.completeOnboarding()
        viewModel.completeOnboarding()

        assertTrue(viewModel.hasCompletedOnboarding())
    }

    @Test
    fun viewModel_afterRestart_readsThePersistedValueFromTheRepository() {
        viewModel.completeOnboarding()

        // Nouveau ViewModel (relancement) sur le même dépôt persistant
        assertTrue(OnboardingViewModel(repository).hasCompletedOnboarding())
    }
}
