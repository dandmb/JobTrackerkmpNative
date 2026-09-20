package com.dmb.jobtracker.domain.repository

/** Mémorise si l'utilisateur a déjà vu (terminé ou passé) l'onboarding, pour ne l'afficher qu'au premier lancement. */
interface OnboardingRepository {
    fun hasCompletedOnboarding(): Boolean
    fun setOnboardingCompleted()
}
