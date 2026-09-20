//
//  SortOption.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

enum SortOption: String, CaseIterable, Identifiable {
    case dateDesc = "Plus récent"
    case dateAsc = "Plus ancien"
    case alphaAsc = "A → Z"
    case alphaDesc = "Z → A"
    var id: String { rawValue }
}
