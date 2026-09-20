//
//  AppRootView.swift
//  iosApp
//

import Combine
import SwiftUI
import SharedLogic

/// Dépendances créées UNE seule fois (`@StateObject` n'évalue son initialiseur qu'une fois par identité de vue) :
/// le ViewModel de la liste sert au gating du splash ET à l'écran principal (pas de double instanciation ni de rechargement).
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

    /// Règle partagée avec Android : splash affiché tant que (durée minimale non écoulée) OU (liste en chargement).
    var keepSplash: Bool {
        let gating = SplashGating.shared
        let elapsed: Int64 = minDurationElapsed ? gating.MIN_DURATION_MILLIS : 0
        return gating.shouldKeepSplash(elapsedMillis: elapsed, isLoading: isListLoading)
    }

    func completeOnboarding() {
        onboardingViewModel.completeOnboarding()   // « Passer » comme « Commencer » : ne plus le montrer
        showOnboarding = false                     // navigation vers la liste, sans redémarrer l'app
    }
}

struct AppRootView: View {
    @StateObject private var model = AppRootModel()

    var body: some View {
        ZStack {
            // Le contenu est présent SOUS le splash : le chargement de la liste se fait pendant qu'il est affiché.
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
