package com.dmb.joblog.presentation.splash

object SplashGating {

    const val MIN_DURATION_MILLIS: Long = 800

    fun shouldKeepSplash(elapsedMillis: Long, isLoading: Boolean): Boolean =
        elapsedMillis < MIN_DURATION_MILLIS || isLoading
}
