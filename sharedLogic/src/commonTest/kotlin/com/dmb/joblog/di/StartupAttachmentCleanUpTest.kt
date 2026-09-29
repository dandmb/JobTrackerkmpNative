package com.dmb.joblog.di

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StartupAttachmentCleanUpTest {

    @Test
    fun severalCalls_runTheCleanUpOnlyOnce() = runTest {
        var runs = 0
        val cleanUp = StartupAttachmentCleanUp { runs++ }

        cleanUp()
        cleanUp()

        assertEquals(1, runs)
    }

    @Test
    fun concurrentCalls_runTheCleanUpOnlyOnce() = runTest {
        var runs = 0
        val cleanUp = StartupAttachmentCleanUp { runs++ }

        List(5) { async { cleanUp() } }.awaitAll()

        assertEquals(1, runs)
    }

    @Test
    fun aFailingCleanUp_isNotRetriedOnTheNextCall() = runTest {
        var runs = 0
        val cleanUp = StartupAttachmentCleanUp { runs++; error("disk") }

        runCatching { cleanUp() }
        runCatching { cleanUp() }

        assertEquals(1, runs)
    }
}
