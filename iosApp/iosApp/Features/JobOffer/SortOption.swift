//
//  SortOption.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import SwiftUI
import SharedLogic

// `SortOption` vient de sharedLogic (comme `ApplicationStatus`) : une seule énumération pour les deux plateformes,
// chacune ajoutant son propre libellé (voir `SortOption.kt` côté Android, `labelRes()`). Kotlin exporte l'énumération
// comme une classe côté Swift (interop Objective-C classique) : elle ne peut pas conformer nativement à
// `CaseIterable`/`Identifiable` (Self.AllCases.Element ≠ Self) ; on énumère avec `.entries` (comme `ApplicationStatus`,
// voir JobOfferCard.swift) et `ForEach(..., id: \.self)` plutôt que `.allCases`.
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

    /// Clé de traduction (`sort_*`), pour les tests (miroir de `nameOf(it.labelRes())` côté Android).
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
