package com.dmb.jobtracker.presentation.splash

/**
 * Règle d'affichage du splash, IDENTIQUE sur Android (`SplashScreen.setKeepOnScreenCondition`) et iOS (`SplashView`) :
 * le splash reste affiché tant que la durée minimale n'est pas écoulée OU que la liste est encore en chargement.
 */
object SplashGating {

    /** Durée minimale d'affichage : évite un clignotement quand les données arrivent quasi instantanément. */
    const val MIN_DURATION_MILLIS: Long = 800

    fun shouldKeepSplash(elapsedMillis: Long, isLoading: Boolean): Boolean =
        elapsedMillis < MIN_DURATION_MILLIS || isLoading
}
