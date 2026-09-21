package com.dmb.joblog.presentation.onboarding

import com.dmb.joblog.domain.repository.OnboardingRepository

/**
 * Même pattern que `JobOfferListViewModel` : classe publique (visible de Swift), constructeur `internal` (instanciée
 * uniquement par Koin). Aucun état observable : la lecture est synchrone et se fait une fois au démarrage de l'app.
 */
class OnboardingViewModel internal constructor(
    private val repository: OnboardingRepository
) {
    fun hasCompletedOnboarding(): Boolean = repository.hasCompletedOnboarding()

    fun completeOnboarding() {
        repository.setOnboardingCompleted()
    }
}
