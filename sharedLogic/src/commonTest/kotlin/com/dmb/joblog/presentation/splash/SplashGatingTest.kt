package com.dmb.joblog.presentation.splash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Règle de gating du splash : affiché tant que (durée minimale non écoulée) OU (liste en chargement). */
class SplashGatingTest {

    private val min = SplashGating.MIN_DURATION_MILLIS

    @Test
    fun minDuration_isEightHundredMilliseconds() {
        assertEquals(800L, min)
    }

    @Test
    fun shouldKeepSplash_beforeMinDurationAndLoading_isTrue() {
        assertTrue(SplashGating.shouldKeepSplash(elapsedMillis = 0, isLoading = true))
    }

    @Test
    fun shouldKeepSplash_beforeMinDurationEvenIfAlreadyLoaded_isTrue() {
        // Données instantanées : on garde quand même le splash jusqu'à la durée minimale (pas de clignotement).
        assertTrue(SplashGating.shouldKeepSplash(elapsedMillis = 100, isLoading = false))
    }

    @Test
    fun shouldKeepSplash_afterMinDurationButStillLoading_isTrue() {
        assertTrue(SplashGating.shouldKeepSplash(elapsedMillis = min + 5_000, isLoading = true))
    }

    @Test
    fun shouldKeepSplash_afterMinDurationAndLoaded_isFalse() {
        assertFalse(SplashGating.shouldKeepSplash(elapsedMillis = min + 1, isLoading = false))
    }

    @Test
    fun shouldKeepSplash_exactlyAtMinDurationAndLoaded_isFalse() {
        assertFalse(SplashGating.shouldKeepSplash(elapsedMillis = min, isLoading = false))
    }

    @Test
    fun shouldKeepSplash_oneMillisecondBeforeMinDurationAndLoaded_isTrue() {
        assertTrue(SplashGating.shouldKeepSplash(elapsedMillis = min - 1, isLoading = false))
    }

    @Test
    fun shouldKeepSplash_negativeElapsedTime_isTreatedAsNotElapsed() {
        assertTrue(SplashGating.shouldKeepSplash(elapsedMillis = -1, isLoading = false))
    }

    @Test
    fun shouldKeepSplash_truthTable_isMinNotElapsedOrLoading() {
        listOf(0L, min - 1, min, min + 1).forEach { elapsed ->
            listOf(true, false).forEach { loading ->
                assertEquals(elapsed < min || loading, SplashGating.shouldKeepSplash(elapsed, loading), "elapsed=$elapsed loading=$loading")
            }
        }
    }
}
