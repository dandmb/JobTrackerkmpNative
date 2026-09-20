//
//  JobOfferListLogic.swift
//  iosApp
//

import Foundation
import SharedLogic

// Recherche puis tri de la liste affichée (extrait tel quel de `JobOfferListView`, pour être testable sans SwiftUI).
// Miroir de `searchedAndSorted` côté Android (ui/joboffer/OfferListLogic.kt).
extension Array where Element == JobOffer {
    func searchedAndSorted(query: String, sortOption: SortOption) -> [JobOffer] {
        let filtered = filter {
            query.isEmpty ||
            $0.title.localizedCaseInsensitiveContains(query) ||
            $0.company.localizedCaseInsensitiveContains(query)
        }
        switch sortOption {
        case .dateDesc: return filtered.sorted { $0.appliedDate.toDate() > $1.appliedDate.toDate() }
        case .dateAsc: return filtered.sorted { $0.appliedDate.toDate() < $1.appliedDate.toDate() }
        case .alphaAsc: return filtered.sorted { $0.title.lowercased() < $1.title.lowercased() }
        case .alphaDesc: return filtered.sorted { $0.title.lowercased() > $1.title.lowercased() }
        }
    }
}
