//
//  StatusFilterRow.swift
//  iosApp
//

import SwiftUI
import SharedLogic

/// Rangée de puces de filtre par statut, sous la barre de recherche (miroir de `StatusFilterRow.kt`, Android : `FilterChip`
/// M3 dans une `Row` à défilement horizontal). Ici : `ScrollView` horizontal de boutons stylés en pilules togglables.
///
/// Sélection MULTIPLE (voir `OfferListFilter`, sharedLogic, qui fait l'union) : « Tous » = ensemble vide, sélectionner un
/// statut l'ajoute à la sélection existante, le retirer l'enlève. Couleur de chaque puce sélectionnée = couleur du
/// statut (comme le badge de `JobOfferStatsCard` : fond teinté à `statusBadgeTintAlpha`, texte dans la couleur du
/// statut) ; « Tous » utilise le teal de marque, cohérent avec les autres actions primaires de l'écran.
struct StatusFilterRow: View {
    @Binding var selectedStatuses: Set<ApplicationStatus>
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                pill(label: L("filter_all"), isSelected: selectedStatuses.isEmpty, tint: .tealPrimary) {
                    selectedStatuses = []
                }
                ForEach(ApplicationStatus.entries, id: \.self) { status in
                    let isSelected = selectedStatuses.contains(status)
                    pill(label: status.displayLabel, isSelected: isSelected, tint: status.color) {
                        if isSelected { selectedStatuses.remove(status) } else { selectedStatuses.insert(status) }
                    }
                }
            }
            .padding(.horizontal, 16)
        }
    }

    @ViewBuilder
    private func pill(label: String, isSelected: Bool, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .appTextStyle(.labelLarge)
                .foregroundStyle(isSelected ? tint : Color.onSurfaceVariant)
                .padding(.horizontal, 14)
                .frame(minHeight: 32)
                .background(isSelected ? tint.opacity(Color.statusBadgeTintAlpha(for: colorScheme)) : Color.clear)
                .overlay(Capsule().strokeBorder(isSelected ? tint : Color.outlineVariant, lineWidth: 1))
                .clipShape(Capsule())
                // HIG : zone tactile >= 44 x 44 pt, comme le chip de statut de JobOfferCard (le pill reste visuellement à 32 pt).
                .frame(minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
