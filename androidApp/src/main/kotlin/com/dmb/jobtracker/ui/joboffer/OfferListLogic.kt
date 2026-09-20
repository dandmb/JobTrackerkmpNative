package com.dmb.jobtracker.ui.joboffer

import com.dmb.jobtracker.domain.model.JobOffer

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
