package com.dmb.jobtracker.ui.joboffer

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfferListLogicTest {

    private fun offer(
        id: Long,
        title: String,
        company: String = "Acme",
        applied: LocalDate = LocalDate(2026, 9, 1),
    ) = JobOffer(
        id = id, title = title, company = company, appliedDate = applied, status = ApplicationStatus.APPLIED,
    )

    private val android = offer(1, "Développeur Android", "Spotify", LocalDate(2026, 9, 5))
    private val ios = offer(2, "iOS Engineer", "eBay", LocalDate(2026, 8, 20))
    private val backend = offer(3, "backend dev", "Zeta", LocalDate(2026, 9, 12))
    private val all = listOf(android, ios, backend)

    private fun List<JobOffer>.ids() = map { it.id }

    // ---------- filtrage ----------

    @Test
    fun searchedAndSorted_emptyQuery_keepsEveryOffer() {
        assertEquals(setOf(1L, 2L, 3L), all.searchedAndSorted("", SortOption.DATE_DESC).ids().toSet())
    }

    @Test
    fun searchedAndSorted_whitespaceOnlyQuery_isTreatedAsNoFilter() {
        assertEquals(3, all.searchedAndSorted("   ", SortOption.DATE_DESC).size)
        assertEquals(3, all.searchedAndSorted("\t", SortOption.DATE_DESC).size)
        assertEquals(3, all.searchedAndSorted(" \n ", SortOption.DATE_DESC).size)
    }

    @Test
    fun searchedAndSorted_emptyList_returnsEmptyList() {
        assertTrue(emptyList<JobOffer>().searchedAndSorted("x", SortOption.ALPHA_ASC).isEmpty())
    }

    @Test
    fun searchedAndSorted_queryMatchingATitle_returnsOnlyThatOffer() {
        assertEquals(listOf(1L), all.searchedAndSorted("android", SortOption.DATE_DESC).ids())
    }

    @Test
    fun searchedAndSorted_queryMatchingACompany_returnsOnlyThatOffer() {
        assertEquals(listOf(3L), all.searchedAndSorted("zeta", SortOption.DATE_DESC).ids())
    }

    @Test
    fun searchedAndSorted_query_isCaseInsensitive() {
        assertEquals(listOf(2L), all.searchedAndSorted("IOS ENGINEER", SortOption.DATE_DESC).ids())
        assertEquals(listOf(2L), all.searchedAndSorted("ebay", SortOption.DATE_DESC).ids())
    }

    @Test
    fun searchedAndSorted_query_matchesPartOfAWord() {
        assertEquals(listOf(1L), all.searchedAndSorted("dévelop", SortOption.DATE_DESC).ids())
    }

    @Test
    fun searchedAndSorted_queryMatchingTitleOfOneAndCompanyOfAnother_returnsBoth() {
        val offers = listOf(offer(1, "Data engineer", "Acme"), offer(2, "Designer", "Data Corp"), offer(3, "Cook", "Zed"))

        assertEquals(setOf(1L, 2L), offers.searchedAndSorted("data", SortOption.DATE_DESC).ids().toSet())
    }

    @Test
    fun searchedAndSorted_queryWithoutAccents_doesNotMatchAccentedTitle() {
        // Caractérisation : ni Android (contains ignoreCase) ni iOS (localizedCaseInsensitiveContains) ne plient les accents.
        assertTrue(all.searchedAndSorted("develop", SortOption.DATE_DESC).isEmpty())
    }

    @Test
    fun searchedAndSorted_queryMatchingNothing_returnsEmptyList() {
        assertTrue(all.searchedAndSorted("cobol", SortOption.DATE_DESC).isEmpty())
    }

    @Test
    fun searchedAndSorted_queryWithSurroundingSpaces_isUsedAsIsWithoutTrimming() {
        // Comportement actuel (caractérisation) : « dev  » avec deux espaces ne trouve pas « backend dev ».
        assertTrue(all.searchedAndSorted("dev  ", SortOption.DATE_DESC).isEmpty())
        assertEquals(listOf(3L), all.searchedAndSorted("end dev", SortOption.DATE_DESC).ids())
    }

    @Test
    fun searchedAndSorted_doesNotSearchInOtherFieldsLikeLocationOrNotes() {
        val offer = JobOffer(
            id = 9, title = "Dev", company = "Acme", location = "Paris", notes = "rappeler lundi",
            appliedDate = LocalDate(2026, 9, 1), status = ApplicationStatus.APPLIED,
        )

        assertTrue(listOf(offer).searchedAndSorted("paris", SortOption.DATE_DESC).isEmpty())
        assertTrue(listOf(offer).searchedAndSorted("lundi", SortOption.DATE_DESC).isEmpty())
    }

    // ---------- tri ----------

    @Test
    fun searchedAndSorted_dateDesc_putsTheMostRecentApplicationFirst() {
        assertEquals(listOf(3L, 1L, 2L), all.searchedAndSorted("", SortOption.DATE_DESC).ids())
    }

    @Test
    fun searchedAndSorted_dateAsc_putsTheOldestApplicationFirst() {
        assertEquals(listOf(2L, 1L, 3L), all.searchedAndSorted("", SortOption.DATE_ASC).ids())
    }

    @Test
    fun searchedAndSorted_alphaAsc_sortsByTitleIgnoringCase() {
        val offers = listOf(offer(1, "banana"), offer(2, "Apple"), offer(3, "cherry"))

        assertEquals(listOf(2L, 1L, 3L), offers.searchedAndSorted("", SortOption.ALPHA_ASC).ids())
    }

    @Test
    fun searchedAndSorted_alphaDesc_sortsByTitleIgnoringCaseInReverse() {
        val offers = listOf(offer(1, "banana"), offer(2, "Apple"), offer(3, "cherry"))

        assertEquals(listOf(3L, 1L, 2L), offers.searchedAndSorted("", SortOption.ALPHA_DESC).ids())
    }

    @Test
    fun searchedAndSorted_dateTies_keepTheOriginalRepositoryOrder() {
        val same = LocalDate(2026, 9, 1)
        val offers = listOf(offer(1, "a", applied = same), offer(2, "b", applied = same), offer(3, "c", applied = same))

        assertEquals(listOf(1L, 2L, 3L), offers.searchedAndSorted("", SortOption.DATE_DESC).ids())
        assertEquals(listOf(1L, 2L, 3L), offers.searchedAndSorted("", SortOption.DATE_ASC).ids())
    }

    @Test
    fun searchedAndSorted_titlesEqualIgnoringCase_keepTheOriginalRepositoryOrder() {
        val offers = listOf(offer(1, "dev"), offer(2, "Dev"), offer(3, "DEV"))

        assertEquals(listOf(1L, 2L, 3L), offers.searchedAndSorted("", SortOption.ALPHA_ASC).ids())
        assertEquals(listOf(1L, 2L, 3L), offers.searchedAndSorted("", SortOption.ALPHA_DESC).ids())
    }

    @Test
    fun searchedAndSorted_alphaSortWithAccentedTitle_ordersByCodePointNotByLocale() {
        // Limite connue (caractérisation) : « École » (é > z en code points) passe après « Zoé » en tri A → Z.
        val offers = listOf(offer(1, "École"), offer(2, "Zoé"), offer(3, "Alpha"))

        assertEquals(listOf(3L, 2L, 1L), offers.searchedAndSorted("", SortOption.ALPHA_ASC).ids())
    }

    // ---------- combinaison ----------

    @Test
    fun searchedAndSorted_filterThenSort_appliesBothInOrder() {
        val offers = listOf(
            offer(1, "Dev C", applied = LocalDate(2026, 9, 1)),
            offer(2, "Dev A", applied = LocalDate(2026, 9, 3)),
            offer(3, "Chef", applied = LocalDate(2026, 9, 2)),
            offer(4, "Dev B", applied = LocalDate(2026, 9, 2)),
        )

        // « dev » retient Dev C (1), Dev A (2), Dev B (4) ; « Chef » (3) est écarté avant le tri.
        assertEquals(listOf(2L, 4L, 1L), offers.searchedAndSorted("dev", SortOption.ALPHA_ASC).ids())
        assertEquals(listOf(1L, 4L, 2L), offers.searchedAndSorted("dev", SortOption.DATE_ASC).ids())
    }

    @Test
    fun searchedAndSorted_doesNotMutateTheInputList() {
        val input = listOf(backend, android, ios)

        input.searchedAndSorted("", SortOption.ALPHA_ASC)

        assertEquals(listOf(backend, android, ios), input)
    }
}
