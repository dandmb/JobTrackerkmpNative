package com.dmb.joblog.ui.joboffer

import com.dmb.joblog.domain.model.JobOffer

sealed interface RestoredOfferTarget {
    data object AwaitingData : RestoredOfferTarget

    data object HiddenBySearch : RestoredOfferTarget

    data class InList(val index: Int) : RestoredOfferTarget
}

fun locateRestoredOffer(
    allOffers: List<JobOffer>,
    visibleOffers: List<JobOffer>,
    offerId: Long,
): RestoredOfferTarget {
    if (allOffers.none { it.id == offerId }) return RestoredOfferTarget.AwaitingData
    val index = visibleOffers.indexOfFirst { it.id == offerId }
    return if (index >= 0) RestoredOfferTarget.InList(index) else RestoredOfferTarget.HiddenBySearch
}

data class ItemBounds(val offset: Int, val size: Int)

fun isItemFullyVisible(item: ItemBounds?, viewportEndOffset: Int): Boolean =
    item != null && item.offset >= 0 && item.offset + item.size <= viewportEndOffset
