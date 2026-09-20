package com.dmb.jobtracker.di

import com.dmb.jobtracker.data.local.OnboardingRepositoryImpl
import com.dmb.jobtracker.domain.repository.OnboardingRepository
import com.dmb.jobtracker.presentation.onboarding.OnboardingViewModel
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val onboardingModule = module {
    single<Settings> { Settings() }   // variante no-arg : SharedPreferences par défaut (Android) / NSUserDefaults (iOS)
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
    factory { OnboardingViewModel(get()) }
}
