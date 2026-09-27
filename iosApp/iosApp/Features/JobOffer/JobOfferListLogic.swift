//
//  JobOfferListLogic.swift
//  iosApp
//

import Foundation
import SharedLogic

// Recherche + filtre de statut + tri de la liste affichée : UNE SEULE implémentation, dans sharedLogic
// (`OfferListFilter.apply`, appelé ici) — avant : cette extension réimplémentait la recherche/tri en Swift, en miroir de
// `OfferListLogic.searchedAndSorted` (Android), le même schéma de duplication qui avait fait diverger silencieusement la
// validation des formulaires par le passé (voir PROJECT_CONTEXT.md §6).
extension Array where Element == JobOffer {
    func filteredForDisplay(query: String, sortOption: SortOption, selectedStatuses: Set<ApplicationStatus> = []) -> [JobOffer] {
        OfferListFilter.shared.apply(offers: self, query: query, sortOption: sortOption, selectedStatuses: selectedStatuses)
    }
}
