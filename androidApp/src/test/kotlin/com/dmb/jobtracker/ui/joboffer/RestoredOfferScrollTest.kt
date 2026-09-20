package com.dmb.jobtracker.ui.joboffer

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Décision « faut-il faire défiler jusqu'à la carte restaurée après Annuler ? » : c'est ici qu'a eu lieu la régression
 * « carte invisible / à moitié visible » de l'intervention précédente.
 */
class RestoredOfferScrollTest {

    private fun offer(id: Long, title: String = "Offre $id", applied: LocalDate = LocalDate(2026, 9, 1)) =
        JobOffer(id = id, title = title, company = "Acme", appliedDate = applied, status = ApplicationStatus.APPLIED)

    // ---------- locateRestoredOffer ----------

    @Test
    fun locateRestoredOffer_offerNotInDataYet_waitsForTheNextEmission() {
        val all = listOf(offer(1), offer(2))

        assertEquals(RestoredOfferTarget.AwaitingData, locateRestoredOffer(all, all, offerId = 3))
    }

    @Test
    fun locateRestoredOffer_emptyData_waitsForTheNextEmission() {
        assertEquals(RestoredOfferTarget.AwaitingData, locateRestoredOffer(emptyList(), emptyList(), offerId = 1))
    }

    @Test
    fun locateRestoredOffer_offerInDataButFilteredOutBySearch_isHiddenBySearch() {
        val all = listOf(offer(1, "Android"), offer(2, "iOS"))
        val visible = all.searchedAndSorted("android", SortOption.DATE_DESC)

        assertEquals(RestoredOfferTarget.HiddenBySearch, locateRestoredOffer(all, visible, offerId = 2))
    }

    @Test
    fun locateRestoredOffer_offerInVisibleList_returnsItsIndex() {
        val all = listOf(offer(1), offer(2), offer(3))

        assertEquals(RestoredOfferTarget.InList(2), locateRestoredOffer(all, all, offerId = 3))
    }

    @Test
    fun locateRestoredOffer_offerAtTopOfList_returnsIndexZero() {
        val all = listOf(offer(1), offer(2))

        assertEquals(RestoredOfferTarget.InList(0), locateRestoredOffer(all, all, offerId = 1))
    }

    @Test
    fun locateRestoredOffer_indexIsTheDisplayedIndexAfterSorting_notTheDataIndex() {
        val oldest = offer(1, applied = LocalDate(2026, 1, 1))
        val newest = offer(2, applied = LocalDate(2026, 9, 1))
        val all = listOf(oldest, newest)
        val visible = all.searchedAndSorted("", SortOption.DATE_DESC)   // newest d'abord

        assertEquals(RestoredOfferTarget.InList(1), locateRestoredOffer(all, visible, offerId = 1))
    }

    // ---------- isItemFullyVisible ----------

    @Test
    fun isItemFullyVisible_itemNotMeasuredYet_isNotVisible() {
        assertFalse(isItemFullyVisible(item = null, viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemInsideTheViewport_isVisible() {
        assertTrue(isItemFullyVisible(ItemBounds(offset = 100, size = 500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemStartingExactlyAtTheTop_isVisible() {
        assertTrue(isItemFullyVisible(ItemBounds(offset = 0, size = 500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemCutAtTheTop_isNotVisible() {
        // Le cas de la régression : carte restaurée en tête, dont seul le bas est visible.
        assertFalse(isItemFullyVisible(ItemBounds(offset = -300, size = 500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemOneUnitAboveTheTop_isNotVisible() {
        assertFalse(isItemFullyVisible(ItemBounds(offset = -1, size = 500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemEndingExactlyAtTheViewportEnd_isVisible() {
        assertTrue(isItemFullyVisible(ItemBounds(offset = 1500, size = 500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemOverflowingByOneUnitAtTheBottom_isNotVisible() {
        assertFalse(isItemFullyVisible(ItemBounds(offset = 1501, size = 500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemMostlyBelowTheViewport_isNotVisible() {
        assertFalse(isItemFullyVisible(ItemBounds(offset = 1900, size = 500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_itemTallerThanTheViewport_isNeverFullyVisible() {
        // Limite documentée : une carte plus haute que l'écran déclenchera toujours un défilement jusqu'à elle.
        assertFalse(isItemFullyVisible(ItemBounds(offset = 0, size = 2500), viewportEndOffset = 2000))
    }

    @Test
    fun isItemFullyVisible_zeroSizedItemInsideTheViewport_isVisible() {
        assertTrue(isItemFullyVisible(ItemBounds(offset = 10, size = 0), viewportEndOffset = 2000))
    }
}
