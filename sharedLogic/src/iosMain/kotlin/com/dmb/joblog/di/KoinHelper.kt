package com.dmb.joblog.di

import com.dmb.joblog.presentation.about.AboutViewModel
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import com.dmb.joblog.presentation.onboarding.OnboardingViewModel
import org.koin.mp.KoinPlatform

class KoinHelper {
    fun jobOfferListViewModel(): JobOfferListViewModel =
        KoinPlatform.getKoin().get()

    fun onboardingViewModel(): OnboardingViewModel =
        KoinPlatform.getKoin().get()

    fun aboutViewModel(): AboutViewModel =
        KoinPlatform.getKoin().get()
}