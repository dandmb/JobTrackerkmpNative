
import Combine
import SwiftUI
import SharedLogic

@MainActor
final class AppRootModel: ObservableObject {
    let listViewModel: JobOfferListViewModel
    private let onboardingViewModel: OnboardingViewModel
    private let listObservable: JobOfferListObservable

    @Published var showOnboarding: Bool
    @Published private(set) var isListLoading: Bool
    @Published var minDurationElapsed = false

    init() {
        let koin = KoinHelper()
        listViewModel = koin.jobOfferListViewModel()
        onboardingViewModel = koin.onboardingViewModel()
        listObservable = JobOfferListObservable(viewModel: listViewModel)
        showOnboarding = !onboardingViewModel.hasCompletedOnboarding()
        isListLoading = listObservable.state.isLoading
        listObservable.$state
            .map { $0.isLoading }
            .removeDuplicates()
            .assign(to: &$isListLoading)
    }

    var keepSplash: Bool {
        let gating = SplashGating.shared
        let elapsed: Int64 = minDurationElapsed ? gating.MIN_DURATION_MILLIS : 0
        return gating.shouldKeepSplash(elapsedMillis: elapsed, isLoading: isListLoading)
    }

    func completeOnboarding() {
        onboardingViewModel.completeOnboarding()
        showOnboarding = false
    }
}

struct AppRootView: View {
    @StateObject private var model = AppRootModel()

    var body: some View {
        ZStack {
            if model.showOnboarding {
                OnboardingView(onFinished: model.completeOnboarding)
            } else {
                JobOfferListView(viewModel: model.listViewModel)
            }
            if model.keepSplash {
                SplashView()
                    .transition(.opacity)
                    .zIndex(1)
            }
        }
        .animation(.easeOut(duration: 0.3), value: model.keepSplash)
        .task {
            try? await Task.sleep(nanoseconds: UInt64(SplashGating.shared.MIN_DURATION_MILLIS) * 1_000_000)
            model.minDurationElapsed = true
        }
    }
}
