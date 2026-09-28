
import SwiftUI
import SharedLogic

// Kotlin exporte l'enum comme une classe côté Swift : ne peut pas conformer à CaseIterable/Identifiable nativement —
// on utilise `.entries` et `ForEach(id: \.self)` plutôt que `.allCases`.
extension SortOption {
    var titleKey: LocalizedStringKey {
        switch self {
        case .dateDesc: return "sort_newest"
        case .dateAsc: return "sort_oldest"
        case .alphaAsc: return "sort_az"
        case .alphaDesc: return "sort_za"
        default: return ""
        }
    }

    var translationKey: String {
        switch self {
        case .dateDesc: return "sort_newest"
        case .dateAsc: return "sort_oldest"
        case .alphaAsc: return "sort_az"
        case .alphaDesc: return "sort_za"
        default: return ""
        }
    }
}
