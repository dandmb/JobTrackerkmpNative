//
//  OnboardingView.swift
//  iosApp
//

import SwiftUI
import SharedLogic

/// Onboarding en 3 pages. Contenu et règles de navigation communs à Android : `OnboardingContent` (sharedLogic).
/// « Passer » et « Commencer » appellent tous deux `onFinished` ; « Suivant » passe à la page suivante.
struct OnboardingView: View {
    let onFinished: () -> Void

    @State private var page = 0

    private let content = OnboardingContent.companion.of(language: AppLanguage.current)
    // Icône de chaque page (même ordre que OnboardingContent.pages) : suivi, statuts, statistiques.
    private let icons = ["briefcase.fill", "flag.fill", "chart.bar.fill"]

    private var pageCount: Int { Int(content.pageCount) }
    private var isLastPage: Bool { content.isLastPage(index: Int32(page)) }

    var body: some View {
        VStack(spacing: 0) {
            // « Passer » : réserve la place sur la dernière page pour éviter un saut de mise en page
            HStack {
                Spacer()
                if content.showsSkip(index: Int32(page)) {
                    Button(content.skipLabel, action: onFinished)
                        .appTextStyle(.labelLarge)
                        .foregroundStyle(Color.tealPrimary)
                        .padding(.trailing, 20)
                }
            }
            .frame(height: 44)

            // .page : balayage horizontal ; les indicateurs sont dessinés à la main (mêmes que sur Android, couleurs de la marque)
            TabView(selection: $page) {
                ForEach(0..<pageCount, id: \.self) { index in
                    OnboardingPageView(
                        systemImage: icons[index],
                        title: content.pages[index].title,
                        description: content.pages[index].description_
                    )
                    .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))

            PageIndicators(count: pageCount, current: page)
                .padding(.bottom, 24)

            Button(action: primaryAction) {
                Text(content.primaryButtonLabel(index: Int32(page)))
                    .appTextStyle(.labelLarge.copy(weight: .semiBold, size: 16))
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: 32)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.tealPrimary)
            .controlSize(.large)
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .background(Color.appBackground.ignoresSafeArea())
    }

    private func primaryAction() {
        if isLastPage {
            onFinished()
        } else {
            withAnimation { page = Int(content.nextPageIndex(index: Int32(page))) }
        }
    }
}

private struct OnboardingPageView: View {
    let systemImage: String
    let title: String
    let description: String

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            ZStack {
                Circle().fill(Color.tealContainer).frame(width: 160, height: 160)
                Image(systemName: systemImage)
                    .font(.system(size: 64))
                    .foregroundStyle(Color.tealOnContainer)
                    .accessibilityHidden(true)      // décorative : le titre porte le sens
            }
            Text(title)
                .appTextStyle(.headlineSmall)
                .foregroundStyle(Color.onAppBackground)
                .multilineTextAlignment(.center)
                .padding(.top, 40)
            Text(description)
                .appTextStyle(.bodyLarge)
                .foregroundStyle(Color.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.top, 12)
            Spacer()
            Spacer()
        }
        .padding(.horizontal, 32)
        .accessibilityElement(children: .combine)
    }
}

private struct PageIndicators: View {
    let count: Int
    let current: Int

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<count, id: \.self) { index in
                Capsule()
                    .fill(index == current ? Color.tealPrimary : Color.outlineVariant)
                    .frame(width: index == current ? 24 : 8, height: 8)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: current)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(L("onboarding_page_indicator", current + 1, count))
    }
}
