package com.dmb.joblog.presentation.onboarding

import com.dmb.joblog.domain.repository.OnboardingRepository

class OnboardingViewModel internal constructor(
    private val repository: OnboardingRepository
) {
    fun hasCompletedOnboarding(): Boolean = repository.hasCompletedOnboarding()

    fun completeOnboarding() {
        repository.setOnboardingCompleted()
    }
}
