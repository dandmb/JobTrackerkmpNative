package com.dmb.joblog.ui.joboffer

import com.dmb.joblog.domain.model.JobOffer

// Recherche + filtre de statut + tri : UNE SEULE implémentation, dans sharedLogic (`presentation.joboffer.OfferListFilter`,
// extension `filteredForDisplay`), appelée par cet écran et par iOS. Elle n'est plus dupliquée ici (voir rapport-filtre-statut.md).

// ---------------------------------------------------------------------------------------------
// Scroll vers la carte restaurée après « Annuler » : la DÉCISION est pure (testable), l'EFFET (LazyListState,
// attente d'un frame, animateScrollToItem) reste dans JobOfferListScreen.
// ---------------------------------------------------------------------------------------------

/** Où en est l'offre restaurée par rapport aux données affichées. */
sealed interface RestoredOfferTarget {
    /** Le ré-ajout n'est pas encore reflété par le flux de données : il faut attendre la prochaine émission. */
    data object AwaitingData : RestoredOfferTarget

    /** L'offre est revenue mais la recherche en cours la masque : rien à montrer. */
    data object HiddenBySearch : RestoredOfferTarget

    /** L'offre est dans la liste affichée, à cet index. */
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

/** Position d'un item mesuré par le LazyColumn (équivalent de `LazyListItemInfo.offset` / `size`). */
data class ItemBounds(val offset: Int, val size: Int)

/**
 * Vrai si l'item est ENTIÈREMENT dans la zone visible. `null` = item pas encore mesuré / hors de la zone composée.
 * Une carte à moitié visible (coupée en haut ou en bas) n'est pas « visible » : il faut alors faire défiler jusqu'à elle.
 */
fun isItemFullyVisible(item: ItemBounds?, viewportEndOffset: Int): Boolean =
    item != null && item.offset >= 0 && item.offset + item.size <= viewportEndOffset
