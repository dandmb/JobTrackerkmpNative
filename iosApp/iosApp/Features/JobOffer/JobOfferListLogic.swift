
import Foundation
import SharedLogic

extension Array where Element == JobOffer {
    func filteredForDisplay(query: String, sortOption: SortOption, selectedStatuses: Set<ApplicationStatus> = []) -> [JobOffer] {
        OfferListFilter.shared.apply(offers: self, query: query, sortOption: sortOption, selectedStatuses: selectedStatuses)
    }
}
