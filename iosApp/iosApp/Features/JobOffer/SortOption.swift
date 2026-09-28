//
//  SortOption.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import SwiftUI

/// Options de tri. La valeur brute est la CLÉ de traduction (`sort_*` dans Localizable.strings), pas un texte.
enum SortOption: String, CaseIterable, Identifiable {
    case dateDesc = "sort_newest"
    case dateAsc = "sort_oldest"
    case alphaAsc = "sort_az"
    case alphaDesc = "sort_za"
    var id: String { rawValue }

    var titleKey: LocalizedStringKey { LocalizedStringKey(rawValue) }
}
