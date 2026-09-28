//
//  SplashView.swift
//  iosApp
//

import SwiftUI

/// Écran de transition affiché juste après le LaunchScreen système (statique par design d'Apple), tant que le gating l'exige.
/// Fond = teal primaire de la marque ; logo = `JobLogMark` (vectoriel, même géométrie que l'icône Android et l'icône de l'app).
struct SplashView: View {
    var body: some View {
        ZStack {
            Color(hex: 0x0D6E68)                      // teal primaire (= Teal40 / tealPrimary en clair), identique clair/sombre
                .ignoresSafeArea()
            JobLogMark()
                .frame(width: 100)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("JobLog")
    }
}
