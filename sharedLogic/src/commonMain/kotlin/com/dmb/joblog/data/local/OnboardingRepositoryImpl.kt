package com.dmb.joblog.data.local

import com.dmb.joblog.domain.repository.OnboardingRepository
import com.russhwolf.settings.Settings

internal const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"

/** Persistance via multiplatform-settings (SharedPreferences sur Android, NSUserDefaults sur iOS). */
internal class OnboardingRepositoryImpl(private val settings: Settings) : OnboardingRepository {

    override fun hasCompletedOnboarding(): Boolean = settings.getBoolean(ONBOARDING_COMPLETED_KEY, false)

    override fun setOnboardingCompleted() {
        settings.putBoolean(ONBOARDING_COMPLETED_KEY, true)
    }
}
