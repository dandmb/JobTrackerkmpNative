package com.dmb.joblog.domain.repository

interface OnboardingRepository {
    fun hasCompletedOnboarding(): Boolean
    fun setOnboardingCompleted()
}
