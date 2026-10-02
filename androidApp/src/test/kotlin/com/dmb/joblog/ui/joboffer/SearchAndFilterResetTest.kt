package com.dmb.joblog.ui.joboffer

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.presentation.joboffer.JobOfferListState
import kotlinx.datetime.LocalDate
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchAndFilterResetTest {

    private fun offer(id: Long) =
        JobOffer(id = id, title = "Offre $id", company = "Acme", appliedDate = LocalDate(2026, 9, 1), status = ApplicationStatus.APPLIED)

    private val stillLoading = JobOfferListState()
    private fun loaded(vararg offers: JobOffer) = JobOfferListState(offers = offers.toList(), isLoading = false)

    private class RestoredScreenState(var searchQuery: String, var selectedStatuses: Set<ApplicationStatus>) {
        fun receive(state: JobOfferListState) {
            if (shouldResetSearchAndFilter(state)) {
                searchQuery = ""
                selectedStatuses = emptySet()
            }
        }
    }

    @Test
    fun shouldResetSearchAndFilter_initialStateBeforeTheDatabaseAnswers_doesNotReset() {
        assertTrue(stillLoading.offers.isEmpty() && stillLoading.isLoading, "état initial attendu : liste vide et chargement en cours")

        assertFalse(shouldResetSearchAndFilter(stillLoading))
    }

    @Test
    fun shouldResetSearchAndFilter_loadedAndTrulyEmpty_resets() {
        assertTrue(shouldResetSearchAndFilter(loaded()))
    }

    @Test
    fun shouldResetSearchAndFilter_loadedWithOffers_doesNotReset() {
        assertFalse(shouldResetSearchAndFilter(loaded(offer(1))))
    }

    @Test
    fun restoredSearchAndFilter_surviveTheLoadingStateThenTheArrivalOfData() {
        val screen = RestoredScreenState(searchQuery = "ios", selectedStatuses = setOf(ApplicationStatus.APPLIED))

        screen.receive(stillLoading)
        screen.receive(loaded(offer(1), offer(2)))

        assertEquals("ios", screen.searchQuery)
        assertEquals(setOf(ApplicationStatus.APPLIED), screen.selectedStatuses)
    }

    @Test
    fun searchAndFilter_areClearedOnceEveryOfferHasBeenDeleted() {
        val screen = RestoredScreenState(searchQuery = "ios", selectedStatuses = setOf(ApplicationStatus.APPLIED))

        screen.receive(loaded(offer(1)))
        screen.receive(loaded())

        assertEquals("", screen.searchQuery)
        assertEquals(emptySet(), screen.selectedStatuses)
    }

    @Test
    fun searchAndFilter_areClearedWhenTheDatabaseTurnsOutToBeEmptyAfterLoading() {
        val screen = RestoredScreenState(searchQuery = "ios", selectedStatuses = setOf(ApplicationStatus.APPLIED))

        screen.receive(stillLoading)
        screen.receive(loaded())

        assertEquals("", screen.searchQuery)
        assertEquals(emptySet(), screen.selectedStatuses)
    }

    @Test
    fun screen_resetsSearchAndFilterThroughTheTestedDecision() {
        val source = listOf("src/main/kotlin", "androidApp/src/main/kotlin")
            .map { File(it, "com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt") }.first { it.exists() }.readText()
        val effect = source.substringAfter("val resetSearchAndFilter = shouldResetSearchAndFilter(state)").substringBefore("StatusBarIconsForPrimaryTopBar")

        assertTrue(effect.trimStart().startsWith("LaunchedEffect(resetSearchAndFilter) {"))
        assertTrue(effect.contains("if (resetSearchAndFilter) {"))
        assertFalse(Regex("""if \(state\.offers\.isEmpty\(\)\) \{\s*searchQuery = """"").containsMatchIn(source), "ancienne condition : elle vidait aussi pendant le chargement")
    }
}
