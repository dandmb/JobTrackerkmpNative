package com.dmb.jobtracker.di

fun sharedModules() = listOf(
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)