package com.dmb.joblog.ui.joboffer

import com.dmb.joblog.domain.model.JobOffer

/**
 * Recherche puis tri de la liste affichée (extrait tel quel de `JobOfferListScreen`, pour être testable sans Compose).
 *
 * - Une recherche vide OU composée uniquement d'espaces ne filtre rien.
 * - La recherche porte sur le titre et l'entreprise, sans tenir compte de la casse.
 * - Les tris sont stables : à critère égal, l'ordre d'origine (celui du repository) est conservé.
 */
fun List<JobOffer>.searchedAndSorted(query: String, sortOption: SortOption): List<JobOffer> =
    filter {
        query.isBlank() ||
            it.title.contains(query, ignoreCase = true) ||
            it.company.contains(query, ignoreCase = true)
    }.let { list ->
        when (sortOption) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.appliedDate }
            SortOption.DATE_ASC -> list.sortedBy { it.appliedDate }
            SortOption.ALPHA_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.ALPHA_DESC -> list.sortedByDescending { it.title.lowercase() }
        }
    }

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
