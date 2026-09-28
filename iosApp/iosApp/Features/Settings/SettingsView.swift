//
//  SettingsView.swift
//  iosApp
//

import SwiftUI
import SharedLogic

/// Écran « Réglages » : point d'entrée unique vers « À propos » et « Politique de confidentialité », atteint depuis
/// l'icône ⚙️ de la barre du haut de la liste (avant cette intervention, l'icône `info.circle` menait directement à
/// « À propos », qui contenait aussi la confidentialité).
struct SettingsView: View {
    private let content = SettingsContent.companion.of(language: AppLanguage.current)

    var body: some View {
        List {
            NavigationLink {
                AboutView()
            } label: {
                Label(content.aboutRowLabel, systemImage: "info.circle")
            }
            NavigationLink {
                PrivacyView()
            } label: {
                Label(content.privacyRowLabel, systemImage: "hand.raised")
            }
        }
        .navigationTitle(content.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
    }
}
