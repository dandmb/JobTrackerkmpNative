package com.dmb.joblog.di

import com.dmb.joblog.data.local.OnboardingRepositoryImpl
import com.dmb.joblog.domain.repository.OnboardingRepository
import com.dmb.joblog.presentation.onboarding.OnboardingViewModel
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val onboardingModule = module {
    single<Settings> { Settings() }   // variante no-arg : SharedPreferences par défaut (Android) / NSUserDefaults (iOS)
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
    factory { OnboardingViewModel(get()) }
}
