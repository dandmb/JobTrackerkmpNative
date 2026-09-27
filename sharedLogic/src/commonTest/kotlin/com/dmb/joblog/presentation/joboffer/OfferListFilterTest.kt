package com.dmb.joblog.presentation.joboffer

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.testutil.jobOffer
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SEULE suite de tests pour la recherche + le filtre de statut + le tri de la liste (voir `OfferListFilter.kt`).
 * Migré de `OfferListLogicTest.kt` (Android) / `JobOfferListLogicTests.swift` (iOS), qui testaient chacun leur propre
 * copie de la recherche/tri : ces deux fichiers sont désormais réduits aux gardes de câblage (la logique est ICI,
 * testée une fois pour les deux plateformes, JVM + iOS simulator).
 */
class OfferListFilterTest {

    private fun offer(
        id: Long,
        title: String,
        company: String = "Acme",
        applied: LocalDate = LocalDate(2026, 9, 1),
        status: ApplicationStatus = ApplicationStatus.APPLIED,
    ) = jobOffer(id = id, title = title, company = company, appliedDate = applied, status = status)

    private val android = offer(1, "Développeur Android", "Spotify", LocalDate(2026, 9, 5))
    private val ios = offer(2, "iOS Engineer", "eBay", LocalDate(2026, 8, 20))
    private val backend = offer(3, "backend dev", "Zeta", LocalDate(2026, 9, 12))
    private val all = listOf(android, ios, backend)

    private fun List<JobOffer>.ids() = map { it.id }

    private fun List<JobOffer>.filtered(
        query: String = "",
        sortOption: SortOption = SortOption.DATE_DESC,
        selectedStatuses: Set<ApplicationStatus> = emptySet(),
    ) = filteredForDisplay(query, sortOption, selectedStatuses)

    // ---------- recherche (migré de OfferListLogicTest / JobOfferListLogicTests) ----------

    @Test
    fun emptyQuery_keepsEveryOffer() {
        assertEquals(setOf(1L, 2L, 3L), all.filtered().ids().toSet())
    }

    @Test
    fun whitespaceOnlyQuery_isTreatedAsNoFilter() {
        assertEquals(3, all.filtered(query = "   ").size)
        assertEquals(3, all.filtered(query = "\t").size)
        assertEquals(3, all.filtered(query = " \n ").size)
    }

    @Test
    fun emptyOfferList_returnsEmptyList() {
        assertTrue(emptyList<JobOffer>().filtered(query = "x", sortOption = SortOption.ALPHA_ASC).isEmpty())
    }

    @Test
    fun queryMatchingATitle_returnsOnlyThatOffer() {
        assertEquals(listOf(1L), all.filtered(query = "android").ids())
    }

    @Test
    fun queryMatchingACompany_returnsOnlyThatOffer() {
        assertEquals(listOf(3L), all.filtered(query = "zeta").ids())
    }

    @Test
    fun query_isCaseInsensitive() {
        assertEquals(listOf(2L), all.filtered(query = "IOS ENGINEER").ids())
        assertEquals(listOf(2L), all.filtered(query = "ebay").ids())
    }

    @Test
    fun query_matchesPartOfAWord() {
        assertEquals(listOf(1L), all.filtered(query = "dévelop").ids())
    }

    @Test
    fun queryMatchingTitleOfOneAndCompanyOfAnother_returnsBoth() {
        val offers = listOf(offer(1, "Data engineer", "Acme"), offer(2, "Designer", "Data Corp"), offer(3, "Cook", "Zed"))

        assertEquals(setOf(1L, 2L), offers.filtered(query = "data").ids().toSet())
    }

    @Test
    fun queryWithoutAccents_doesNotMatchAccentedTitle() {
        // Caractérisation (limite connue, commune aux deux plateformes désormais qu'il n'y a plus qu'une implémentation) :
        // ni `contains(ignoreCase = true)` ni son équivalent iOS ne plient les accents.
        assertTrue(all.filtered(query = "develop").isEmpty())
    }

    @Test
    fun queryMatchingNothing_returnsEmptyList() {
        assertTrue(all.filtered(query = "cobol").isEmpty())
    }

    @Test
    fun queryWithSurroundingSpaces_isUsedAsIsWithoutTrimming() {
        // Comportement actuel (caractérisation) : « dev  » avec deux espaces ne trouve pas « backend dev ».
        assertTrue(all.filtered(query = "dev  ").isEmpty())
        assertEquals(listOf(3L), all.filtered(query = "end dev").ids())
    }

    @Test
    fun doesNotSearchInOtherFieldsLikeLocationOrNotes() {
        val offer = jobOffer(
            id = 9, title = "Dev", company = "Acme", location = "Paris", notes = "rappeler lundi",
            appliedDate = LocalDate(2026, 9, 1), status = ApplicationStatus.APPLIED,
        )

        assertTrue(listOf(offer).filtered(query = "paris").isEmpty())
        assertTrue(listOf(offer).filtered(query = "lundi").isEmpty())
    }

    // ---------- tri (migré) ----------

    @Test
    fun dateDesc_putsTheMostRecentApplicationFirst() {
        assertEquals(listOf(3L, 1L, 2L), all.filtered(sortOption = SortOption.DATE_DESC).ids())
    }

    @Test
    fun dateAsc_putsTheOldestApplicationFirst() {
        assertEquals(listOf(2L, 1L, 3L), all.filtered(sortOption = SortOption.DATE_ASC).ids())
    }

    @Test
    fun alphaAsc_sortsByTitleIgnoringCase() {
        val offers = listOf(offer(1, "banana"), offer(2, "Apple"), offer(3, "cherry"))

        assertEquals(listOf(2L, 1L, 3L), offers.filtered(sortOption = SortOption.ALPHA_ASC).ids())
    }

    @Test
    fun alphaDesc_sortsByTitleIgnoringCaseInReverse() {
        val offers = listOf(offer(1, "banana"), offer(2, "Apple"), offer(3, "cherry"))

        assertEquals(listOf(3L, 1L, 2L), offers.filtered(sortOption = SortOption.ALPHA_DESC).ids())
    }

    @Test
    fun dateTies_keepTheOriginalRepositoryOrder() {
        val same = LocalDate(2026, 9, 1)
        val offers = listOf(offer(1, "a", applied = same), offer(2, "b", applied = same), offer(3, "c", applied = same))

        assertEquals(listOf(1L, 2L, 3L), offers.filtered(sortOption = SortOption.DATE_DESC).ids())
        assertEquals(listOf(1L, 2L, 3L), offers.filtered(sortOption = SortOption.DATE_ASC).ids())
    }

    @Test
    fun titlesEqualIgnoringCase_keepTheOriginalRepositoryOrder() {
        val offers = listOf(offer(1, "dev"), offer(2, "Dev"), offer(3, "DEV"))

        assertEquals(listOf(1L, 2L, 3L), offers.filtered(sortOption = SortOption.ALPHA_ASC).ids())
        assertEquals(listOf(1L, 2L, 3L), offers.filtered(sortOption = SortOption.ALPHA_DESC).ids())
    }

    @Test
    fun alphaSortWithAccentedTitle_ordersByCodePointNotByLocale() {
        // Limite connue (caractérisation) : « École » (é > z en code points) passe après « Zoé » en tri A → Z.
        val offers = listOf(offer(1, "École"), offer(2, "Zoé"), offer(3, "Alpha"))

        assertEquals(listOf(3L, 2L, 1L), offers.filtered(sortOption = SortOption.ALPHA_ASC).ids())
    }

    @Test
    fun filterThenSort_appliesBothInOrder() {
        val offers = listOf(
            offer(1, "Dev C", applied = LocalDate(2026, 9, 1)),
            offer(2, "Dev A", applied = LocalDate(2026, 9, 3)),
            offer(3, "Chef", applied = LocalDate(2026, 9, 2)),
            offer(4, "Dev B", applied = LocalDate(2026, 9, 2)),
        )

        // « dev » retient Dev C (1), Dev A (2), Dev B (4) ; « Chef » (3) est écarté avant le tri.
        assertEquals(listOf(2L, 4L, 1L), offers.filtered(query = "dev", sortOption = SortOption.ALPHA_ASC).ids())
        assertEquals(listOf(1L, 4L, 2L), offers.filtered(query = "dev", sortOption = SortOption.DATE_ASC).ids())
    }

    @Test
    fun doesNotMutateTheInputList() {
        val input = listOf(backend, android, ios)

        input.filtered(sortOption = SortOption.ALPHA_ASC)

        assertEquals(listOf(backend, android, ios), input)
    }

    // ---------- filtre de statut (nouveau) ----------

    private val statusOffers = listOf(
        offer(1, "Dev A", status = ApplicationStatus.PENDING),
        offer(2, "Dev B", status = ApplicationStatus.APPLIED),
        offer(3, "Dev C", status = ApplicationStatus.INTERVIEW),
        offer(4, "Dev D", status = ApplicationStatus.REJECTED),
        offer(5, "Dev E", status = ApplicationStatus.ACCEPTED),
    )

    @Test
    fun noStatusSelected_isTreatedAsNoFilter_everyOfferPasses() {
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), statusOffers.filtered(selectedStatuses = emptySet()).ids().toSet())
    }

    @Test
    fun oneStatusSelected_keepsOnlyOffersWithThatStatus() {
        assertEquals(listOf(3L), statusOffers.filtered(selectedStatuses = setOf(ApplicationStatus.INTERVIEW)).ids())
    }

    @Test
    fun oneStatusSelected_matchingNoOffer_returnsEmptyList() {
        val offers = listOf(offer(1, "Dev A", status = ApplicationStatus.PENDING))

        assertTrue(offers.filtered(selectedStatuses = setOf(ApplicationStatus.ACCEPTED)).isEmpty())
    }

    @Test
    fun severalStatusesSelected_keepsTheUnionOfMatchingOffers() {
        val selected = setOf(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED)

        assertEquals(setOf(1L, 5L), statusOffers.filtered(selectedStatuses = selected).ids().toSet())
    }

    @Test
    fun allFiveStatusesSelected_behavesLikeNoFilter() {
        val selected = ApplicationStatus.entries.toSet()

        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), statusOffers.filtered(selectedStatuses = selected).ids().toSet())
    }

    @Test
    fun allStatusChip_meansAnEmptySelection_soItResetsAnyPreviousStatusFilter() {
        // « Tous » ne mémorise aucun état à part vider la sélection : la fonction est pure, donc appeler avec un
        // ensemble vide donne exactement le résultat « aucun filtre », qu'un filtre ait été actif juste avant ou non.
        assertEquals(1, statusOffers.filtered(selectedStatuses = setOf(ApplicationStatus.INTERVIEW)).size)
        assertEquals(5, statusOffers.filtered(selectedStatuses = emptySet()).size)
        assertEquals(statusOffers.filtered(selectedStatuses = emptySet()).ids(), statusOffers.filtered().ids())
    }

    @Test
    fun statusFilter_combinesWithSearch_bothMustMatch_logicalAnd() {
        val offers = listOf(
            offer(1, "Android dev", "Spotify", status = ApplicationStatus.INTERVIEW),
            offer(2, "Android dev", "Meta", status = ApplicationStatus.REJECTED),
            offer(3, "Backend dev", "Spotify", status = ApplicationStatus.INTERVIEW),
        )

        // « android » ET statut = INTERVIEW : seule l'offre 1 matche les deux critères à la fois.
        assertEquals(listOf(1L), offers.filtered(query = "android", selectedStatuses = setOf(ApplicationStatus.INTERVIEW)).ids())
    }

    @Test
    fun statusFilter_combinesWithSearch_matchingSearchButWrongStatus_isExcluded() {
        val offers = listOf(offer(1, "Android dev", status = ApplicationStatus.REJECTED))

        assertTrue(offers.filtered(query = "android", selectedStatuses = setOf(ApplicationStatus.INTERVIEW)).isEmpty())
    }

    @Test
    fun statusFilter_combinesWithSearch_matchingStatusButWrongSearch_isExcluded() {
        val offers = listOf(offer(1, "Android dev", status = ApplicationStatus.INTERVIEW))

        assertTrue(offers.filtered(query = "backend", selectedStatuses = setOf(ApplicationStatus.INTERVIEW)).isEmpty())
    }

    @Test
    fun statusFilter_thenSort_sortsOnlyTheFilteredSubset() {
        val offers = listOf(
            offer(1, "Zebra", applied = LocalDate(2026, 9, 1), status = ApplicationStatus.INTERVIEW),
            offer(2, "Alpha", applied = LocalDate(2026, 9, 2), status = ApplicationStatus.REJECTED),
            offer(3, "Mango", applied = LocalDate(2026, 9, 3), status = ApplicationStatus.INTERVIEW),
        )

        assertEquals(listOf(3L, 1L), offers.filtered(sortOption = SortOption.ALPHA_ASC, selectedStatuses = setOf(ApplicationStatus.INTERVIEW)).ids())
    }

    @Test
    fun statusFilter_doesNotMutateTheInputList() {
        val input = listOf(backend, android, ios)

        input.filtered(selectedStatuses = setOf(ApplicationStatus.APPLIED))

        assertEquals(listOf(backend, android, ios), input)
    }

    // ---------- OfferListFilter.apply (objet appelé depuis Swift) : même résultat que l'extension côté Kotlin ----------

    @Test
    fun offerListFilterApply_andTheKotlinExtension_giveTheSameResult() {
        val direct = OfferListFilter.apply(statusOffers, "dev", SortOption.ALPHA_DESC, setOf(ApplicationStatus.PENDING, ApplicationStatus.REJECTED))
        val extension = statusOffers.filtered(query = "dev", sortOption = SortOption.ALPHA_DESC, selectedStatuses = setOf(ApplicationStatus.PENDING, ApplicationStatus.REJECTED))

        assertEquals(direct, extension)
    }

    @Test
    fun offerListFilterApply_defaultSelectedStatuses_isEmptySet_noFilter() {
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), OfferListFilter.apply(statusOffers, "", SortOption.DATE_DESC).ids().toSet())
    }
}
