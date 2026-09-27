//
//  PrivacyView.swift
//  iosApp
//

import SwiftUI
import SharedLogic

/// Écran « Politique de confidentialité », séparé de « À propos » (voir rapport-reglages-confidentialite.md).
/// Le texte français vient VERBATIM de `PrivacyContent` (sharedLogic) — cet écran ne fait que le mettre en forme,
/// comme `AboutView`.
struct PrivacyView: View {
    private let content = PrivacyContent.companion.of(language: AppLanguage.current)
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(content.documentTitle)
                    .appTextStyle(.titleLarge)
                    .foregroundStyle(Color.onAppBackground)
                Text(content.lastUpdatedLabel)
                    .appTextStyle(.labelLarge)
                    .foregroundStyle(Color.onSurfaceVariant)
                Text(content.introText)
                    .appTextStyle(.bodyLarge)
                    .foregroundStyle(Color.onAppBackground)

                ForEach(Array(content.sections.enumerated()), id: \.offset) { _, section in
                    sectionView(section)
                }

                onlineVersionLink
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(content.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func sectionView(_ section: AboutSection) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(section.title)
                .appTextStyle(.titleMedium)
                .foregroundStyle(Color.onAppBackground)
                .accessibilityAddTraits(.isHeader)
            if let intro = section.intro {
                Text(intro).appTextStyle(.bodyMedium).foregroundStyle(Color.onAppBackground)
            }
            ForEach(Array(section.bullets.enumerated()), id: \.offset) { _, bullet in
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("•")
                    Text(bullet)
                }
                .appTextStyle(.bodyMedium)
                .foregroundStyle(Color.onAppBackground)
            }
            if let note = section.note {
                Text(note).appTextStyle(.bodyMedium).foregroundStyle(Color.onSurfaceVariant)
            }
        }
    }

    /// Lien vers la version en ligne (Gist), tenue identique à ce texte — condition explicite de la demande.
    private var onlineVersionLink: some View {
        Button {
            if let url = URL(string: PrivacyContent.companion.ONLINE_URL) { openURL(url) }
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "arrow.up.right.square")
                Text(content.onlineVersionLabel).underline()
            }
            .appTextStyle(.titleMedium)
            .foregroundStyle(Color.tealOnContainer)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.tealContainer, in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityHint(content.onlineVersionHint)
    }
}
