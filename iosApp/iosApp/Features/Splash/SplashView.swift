//
//  SplashView.swift
//  iosApp
//

import SwiftUI

/// Écran de transition affiché juste après le LaunchScreen système (statique par design d'Apple), tant que le gating l'exige.
/// Fond = teal primaire de la marque. L'icône est un PLACEHOLDER (SF Symbol) à remplacer par le vrai logo.
struct SplashView: View {
    var body: some View {
        ZStack {
            Color(hex: 0x0D6E68)                      // teal primaire (= Teal40 / tealPrimary en clair), identique clair/sombre
                .ignoresSafeArea()
            Image(systemName: "briefcase.fill")
                .font(.system(size: 72))
                .foregroundStyle(.white)
                .accessibilityHidden(true)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("JobTracker")
    }
}
