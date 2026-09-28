
import SwiftUI
import SharedLogic

struct OnboardingView: View {
    let onFinished: () -> Void

    @State private var page = 0

    private let content = OnboardingContent.companion.of(language: AppLanguage.current)
    // ⚠️ À régénérer si l'UI des écrans montrés change.
    private let images = ["Onboarding1", "Onboarding2", "Onboarding3"]

    private var pageCount: Int { Int(content.pageCount) }
    private var isLastPage: Bool { content.isLastPage(index: Int32(page)) }

    var body: some View {
        VStack(spacing: 0) {
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

            TabView(selection: $page) {
                ForEach(0..<pageCount, id: \.self) { index in
                    OnboardingPageView(
                        imageName: images[index],
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
    let imageName: String
    let title: String
    let description: String

    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            Image(imageName)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(maxWidth: 280)
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(Color.outlineVariant, lineWidth: 1))
                .shadow(color: .black.opacity(0.12), radius: 8, y: 4)
                .accessibilityHidden(true)
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
