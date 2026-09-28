package com.dmb.joblog.di

fun sharedModules() = listOf(
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule,
    onboardingModule
)