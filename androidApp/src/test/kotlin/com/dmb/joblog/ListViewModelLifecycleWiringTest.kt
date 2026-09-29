package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListViewModelLifecycleWiringTest {

    private val activity = listOf(
        "src/main/kotlin/com/dmb/joblog/MainActivity.kt",
        "androidApp/src/main/kotlin/com/dmb/joblog/MainActivity.kt",
    ).map(::File).first { it.exists() }.readText()

    @Test
    fun listViewModel_isHeldByAnAndroidViewModel_soItSurvivesARotation() {
        assertTrue(activity.contains("by viewModels"), "le ViewModel de liste doit être conservé à travers les changements de configuration")
        assertFalse(activity.contains("jobOfferListViewModel: JobOfferListViewModel by inject()"),
            "by inject() + factory Koin = un nouveau ViewModel à chaque rotation")
    }

    @Test
    fun listViewModel_isStoppedWhenItsHolderIsCleared_notOnEveryDestroy() {
        assertTrue(activity.contains("override fun onCleared()") && activity.contains("viewModel.onCleared()"))
        assertFalse(activity.contains("override fun onDestroy()"), "l'arrêt est porté par le holder, plus par onDestroy")
    }
}
