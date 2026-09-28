package com.dmb.joblog.ui.joboffer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatusFilterWiringTest {

    private fun readSource(relative: String): String =
        listOf("src/main/kotlin/$relative", "androidApp/src/main/kotlin/$relative")
            .map(::File).first { it.exists() }.readText()

    private val screen = readSource("com/dmb/joblog/ui/joboffer/JobOfferListScreen.kt")
    private val filterRow = readSource("com/dmb/joblog/ui/joboffer/StatusFilterRow.kt")

    @Test
    fun screen_computesVisibleOffers_byCallingTheSharedFilterFunction() {
        assertTrue(screen.contains("import com.dmb.joblog.presentation.joboffer.filteredForDisplay"))
        assertTrue(screen.contains(".filteredForDisplay(searchQuery, sortOption, selectedStatuses)"))
    }

    @Test
    fun screen_noLongerHasItsOwnSearchedAndSortedImplementation() {
        assertFalse(screen.contains("fun List<JobOffer>.searchedAndSorted"), "la recherche/tri ne doit plus être dupliquée localement")
    }

    @Test
    fun offerListLogic_noLongerDeclaresSearchedAndSorted() {
        val logic = readSource("com/dmb/joblog/ui/joboffer/OfferListLogic.kt")
        assertFalse(logic.contains("fun List<JobOffer>.searchedAndSorted"), "logique déplacée dans sharedLogic (OfferListFilter)")
    }

    @Test
    fun statusFilterRow_usesFilterChip_notAssistChip() {
        assertTrue(filterRow.contains("FilterChip("), "sélection togglée : FilterChip, pas AssistChip")
        assertFalse(filterRow.contains("AssistChip("))
    }

    @Test
    fun statusFilterRow_hasOneChipPerStatus_plusAnAllChip() {
        assertTrue(filterRow.contains("stringResource(R.string.filter_all)"), "puce « Tous »")
        assertTrue(filterRow.contains("ApplicationStatus.entries.forEach"), "une puce par statut du domaine, aucun codé en dur")
    }

    @Test
    fun statusFilterRow_scrollsHorizontally_insteadOfWrapping() {
        assertTrue(filterRow.contains("horizontalScroll"), "défilement horizontal si les puces ne tiennent pas sur une ligne")
    }

    @Test
    fun statusFilterRow_selectedChipColor_matchesTheStatusColor() {
        val chipBlock = filterRow.substringAfter("ApplicationStatus.entries.forEach")
        assertTrue(chipBlock.contains("selectedContainerColor = status.color()"))
        assertTrue(chipBlock.contains("selectedLabelColor = status.color()"))
    }

    @Test
    fun allChip_selectsAnEmptySet_resettingEveryOtherChip() {
        val allChipBlock = filterRow.substringBefore("ApplicationStatus.entries.forEach")
        assertTrue(allChipBlock.contains("onSelectionChanged(emptySet())"))
        assertTrue(allChipBlock.contains("selected = selectedStatuses.isEmpty()"))
    }

    @Test
    fun statusChip_toggling_addsOrRemovesFromTheSelection_neverReplacesIt() {
        val chipBlock = filterRow.substringAfter("ApplicationStatus.entries.forEach")
        assertTrue(chipBlock.contains("selectedStatuses - status"), "désélection : retire seulement ce statut")
        assertTrue(chipBlock.contains("selectedStatuses + status"), "sélection : ajoute à la sélection existante (multi-statuts)")
    }

    @Test
    fun statusFilterRow_isInsertedRightAfterTheSearchField_underTheSameEmptinessGuard() {
        val searchField = screen.indexOf("R.string.list_search_placeholder")
        val filterRowCall = screen.indexOf("StatusFilterRow(")
        val guardStart = screen.lastIndexOf("if (state.offers.isNotEmpty()) {", searchField)
        val guardEnd = screen.indexOf("\n            }", searchField)

        assertTrue(guardStart in 0 until searchField, "le champ de recherche doit être dans un garde « offres non vides »")
        assertTrue(searchField in 0 until filterRowCall, "la rangée de filtre doit être sous le champ de recherche")
        assertTrue(filterRowCall in searchField until guardEnd, "la rangée de filtre doit être masquée en même temps que la recherche (liste vide)")
    }

    @Test
    fun selectedStatuses_isResetWhenTheOfferListBecomesEmpty() {
        val resetBlock = screen.substringAfter("LaunchedEffect(state.offers.isEmpty())")
        assertTrue(resetBlock.substringBefore("}").contains("selectedStatuses = emptySet()"))
    }

    @Test
    fun emptyFilterResult_showsADedicatedMessage_priorOverTheSearchMessage() {
        val noResultsBlock = screen.substringAfter("key = \"no-results\"").substringBefore("items(visibleOffers")

        assertTrue(noResultsBlock.contains("R.string.list_no_results_filter"))
        assertTrue(noResultsBlock.contains("selectedStatuses.isNotEmpty()"), "le message de filtre doit être prioritaire quand un statut est sélectionné")
    }

    @Test
    fun sortAction_staysGatedOnAllOffers_notOnTheFilteredSelection() {
        val topBar = screen.substringBefore("floatingActionButton")
        assertTrue(topBar.contains("if (state.offers.isNotEmpty()) {"), "condition inchangée : toutes les offres, pas la liste filtrée")
        assertFalse(topBar.contains("if (visibleOffers.isNotEmpty())"))
    }
}
