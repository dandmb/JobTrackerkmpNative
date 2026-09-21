package com.dmb.joblog.testutil

import com.dmb.joblog.domain.repository.OnboardingRepository

/** Dépôt d'onboarding en mémoire qui compte les appels. */
class FakeOnboardingRepository(var completed: Boolean = false) : OnboardingRepository {

    var hasCompletedCalls = 0
    var setCompletedCalls = 0

    override fun hasCompletedOnboarding(): Boolean {
        hasCompletedCalls++
        return completed
    }

    override fun setOnboardingCompleted() {
        setCompletedCalls++
        completed = true
    }
}
