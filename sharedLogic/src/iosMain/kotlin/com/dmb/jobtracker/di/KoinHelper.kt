package com.dmb.jobtracker.di

import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import com.dmb.jobtracker.presentation.onboarding.OnboardingViewModel
import org.koin.mp.KoinPlatform

class KoinHelper {
    fun jobOfferListViewModel(): JobOfferListViewModel =
        KoinPlatform.getKoin().get()

    fun onboardingViewModel(): OnboardingViewModel =
        KoinPlatform.getKoin().get()
}