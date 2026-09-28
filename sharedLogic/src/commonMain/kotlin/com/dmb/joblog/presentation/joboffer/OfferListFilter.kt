package com.dmb.joblog.presentation.joboffer

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer

enum class SortOption {
    DATE_DESC, DATE_ASC, ALPHA_ASC, ALPHA_DESC
}

/** Recherche + filtre de statut + tri de la liste : SEULE implémentation, partagée par Android et iOS (voir PROJECT_CONTEXT.md §6). */
object OfferListFilter {
    fun apply(
        offers: List<JobOffer>,
        query: String,
        sortOption: SortOption,
        selectedStatuses: Set<ApplicationStatus> = emptySet(),
    ): List<JobOffer> =
        offers.filter {
            (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.company.contains(query, ignoreCase = true)) &&
                (selectedStatuses.isEmpty() || it.status in selectedStatuses)
        }.let { list ->
            when (sortOption) {
                SortOption.DATE_DESC -> list.sortedByDescending { it.appliedDate }
                SortOption.DATE_ASC -> list.sortedBy { it.appliedDate }
                SortOption.ALPHA_ASC -> list.sortedBy { it.title.lowercase() }
                SortOption.ALPHA_DESC -> list.sortedByDescending { it.title.lowercase() }
            }
        }
}

fun List<JobOffer>.filteredForDisplay(
    query: String,
    sortOption: SortOption,
    selectedStatuses: Set<ApplicationStatus> = emptySet(),
): List<JobOffer> = OfferListFilter.apply(this, query, sortOption, selectedStatuses)
