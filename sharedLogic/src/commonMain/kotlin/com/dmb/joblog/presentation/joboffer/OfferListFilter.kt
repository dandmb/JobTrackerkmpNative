package com.dmb.joblog.presentation.joboffer

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer

/**
 * Options de tri de la liste des candidatures. Mêmes valeurs des deux côtés (comme `ApplicationStatus`) : le libellé
 * affiché est une ressource par plateforme, résolue par chaque UI (Android : `SortOption.labelRes()` dans
 * `ui/joboffer/SortOption.kt` ; iOS : `SortOption.titleKey` dans `Features/JobOffer/SortOption.swift`).
 */
enum class SortOption {
    DATE_DESC, DATE_ASC, ALPHA_ASC, ALPHA_DESC
}

/**
 * Recherche + filtre de statut + tri de la liste affichée : SEULE implémentation, partagée par Android et iOS.
 *
 * Avant cette fonction, la recherche et le tri existaient en DEUX implémentations indépendantes extraites de chaque UI
 * (`OfferListLogic.searchedAndSorted` côté Android, son miroir Swift côté iOS) — exactement le schéma de duplication
 * qui avait fait diverger silencieusement la validation des formulaires par le passé (voir PROJECT_CONTEXT.md §6).
 * `OfferListFilter.apply` est l'objet appelé depuis Swift (une extension Kotlin sur `List<T>` n'est pas idiomatique à
 * appeler depuis Objective-C/Swift) ; l'extension [filteredForDisplay] ci-dessous n'est qu'un raccourci d'écriture côté
 * Android qui délègue au même code, sans le dupliquer.
 *
 * Ordre des opérations : recherche PUIS filtre de statut (combinés en ET) PUIS tri.
 * - Une recherche vide OU composée uniquement d'espaces ne filtre rien (comportement inchangé).
 * - La recherche porte sur le titre et l'entreprise, sans tenir compte de la casse.
 * - `selectedStatuses` vide = aucun filtre de statut actif (tout passe, comportement inchangé) ; non vide = ne garde
 *   que les offres dont le statut est dans l'ensemble sélectionné.
 * - Les tris sont stables : à critère égal, l'ordre d'origine (celui du repository) est conservé.
 */
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

/** Raccourci Android (`offers.filteredForDisplay(...)`) : délègue à [OfferListFilter.apply], ne duplique rien. */
fun List<JobOffer>.filteredForDisplay(
    query: String,
    sortOption: SortOption,
    selectedStatuses: Set<ApplicationStatus> = emptySet(),
): List<JobOffer> = OfferListFilter.apply(this, query, sortOption, selectedStatuses)
