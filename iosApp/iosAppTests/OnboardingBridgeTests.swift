import XCTest
import SharedLogic
@testable import JobLog

/// Pont Swift ↔ Kotlin du splash et de l'onboarding. La logique (persistance, contenu, gating) est testée dans sharedLogic
/// (JVM + natif) ; ici on vérifie que Swift l'appelle correctement et que l'app la câble comme prévu.
///
/// NB : `completeOnboarding()` n'est volontairement PAS appelé : il écrirait dans les vraies préférences (NSUserDefaults)
/// de l'app hôte sur le simulateur et ferait sauter l'onboarding au prochain lancement.
final class OnboardingBridgeTests: XCTestCase {

    // MARK: KoinHelper / OnboardingViewModel

    func test_koinHelper_onboardingViewModel_returnsAViewModelFromTheRunningKoin() {
        XCTAssertNotNil(KoinHelper().onboardingViewModel())
    }

    func test_koinHelper_onboardingViewModel_returnsANewInstanceEachTime() {
        XCTAssertFalse(KoinHelper().onboardingViewModel() === KoinHelper().onboardingViewModel(), "le module Koin déclare le ViewModel en factory")
    }

    func test_onboardingViewModel_hasCompletedOnboarding_isReadableFromSwift() {
        // La valeur dépend de l'état réel du simulateur : on vérifie seulement que l'appel aboutit et que deux lectures concordent.
        let viewModel = KoinHelper().onboardingViewModel()

        XCTAssertEqual(viewModel.hasCompletedOnboarding(), KoinHelper().onboardingViewModel().hasCompletedOnboarding())
    }

    // MARK: OnboardingContent vu depuis Swift

    func test_onboardingContent_hasThreePagesWithTheSharedTexts_inEachLanguage() {
        let fr = OnboardingContent.companion.of(language: AppLanguage.fr)
        let en = OnboardingContent.companion.of(language: AppLanguage.en)

        XCTAssertEqual(fr.pageCount, 3)
        XCTAssertEqual(fr.pages.map { $0.title }, ["Suis tes candidatures", "Garde un œil sur les statuts", "Vois où tu en es"])
        XCTAssertEqual(en.pages.map { $0.title }, ["Track your applications", "Keep an eye on statuses", "See where you stand"])
        XCTAssertTrue(fr.pages.allSatisfy { !$0.description_.isEmpty } && en.pages.allSatisfy { !$0.description_.isEmpty })
    }

    func test_onboardingContent_navigationRules_matchTheSharedContract() {
        let fr = OnboardingContent.companion.of(language: AppLanguage.fr)
        let en = OnboardingContent.companion.of(language: AppLanguage.en)

        for content in [fr, en] {
            XCTAssertTrue(content.showsSkip(index: 0))
            XCTAssertTrue(content.showsSkip(index: 1))
            XCTAssertFalse(content.showsSkip(index: 2))
            XCTAssertEqual(content.nextPageIndex(index: 1), 2)
            XCTAssertEqual(content.nextPageIndex(index: 2), 2)
        }
        XCTAssertEqual([fr.primaryButtonLabel(index: 0), fr.primaryButtonLabel(index: 2), fr.skipLabel], ["Suivant", "Commencer", "Passer"])
        XCTAssertEqual([en.primaryButtonLabel(index: 0), en.primaryButtonLabel(index: 2), en.skipLabel], ["Next", "Get started", "Skip"])
    }

    // MARK: SplashGating vu depuis Swift

    func test_splashGating_minDuration_isEightHundredMilliseconds() {
        XCTAssertEqual(SplashGating.shared.MIN_DURATION_MILLIS, 800)
    }

    func test_splashGating_truthTable_isMinNotElapsedOrLoading() {
        let gating = SplashGating.shared
        let min = gating.MIN_DURATION_MILLIS

        for elapsed in [Int64(0), min - 1, min, min + 1] {
            for loading in [true, false] {
                XCTAssertEqual(gating.shouldKeepSplash(elapsedMillis: elapsed, isLoading: loading), elapsed < min || loading, "elapsed=\(elapsed) loading=\(loading)")
            }
        }
    }

    // MARK: garde-fous de câblage (lecture du source, comme les autres gardes du projet)

    private func source(_ relativePath: String) -> String {
        let projectDir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        return (try? String(contentsOf: projectDir.appendingPathComponent("iosApp/\(relativePath)"), encoding: .utf8)) ?? ""
    }

    func test_app_showsTheRootViewInsteadOfTheListDirectly() {
        let app = source("iOSApp.swift")

        XCTAssertFalse(app.isEmpty, "source introuvable (chemin #filePath)")
        XCTAssertTrue(app.contains("AppRootView()"))
        XCTAssertFalse(app.contains("JobOfferListView("), "l'écran de liste ne doit plus être instancié directement par l'App")
    }

    func test_listViewModel_isCreatedOnlyOnceAtTheRoot() {
        let files = ["iOSApp.swift", "Features/App/AppRootView.swift", "Features/JobOffer/JobOfferListView.swift",
                     "Features/Onboarding/OnboardingView.swift", "Features/Splash/SplashView.swift"]
        let occurrences = files.map { source($0).components(separatedBy: "jobOfferListViewModel()").count - 1 }

        XCTAssertEqual(occurrences.reduce(0, +), 1, "une seule création du ViewModel de la liste (par fichier : \(occurrences))")
        XCTAssertEqual(source("Features/App/AppRootView.swift").components(separatedBy: "jobOfferListViewModel()").count - 1, 1)
    }

    func test_root_passesTheAlreadyCreatedViewModelToTheList() {
        XCTAssertTrue(source("Features/App/AppRootView.swift").contains("JobOfferListView(viewModel: model.listViewModel)"))
    }

    func test_root_appliesTheSharedSplashGatingRule() {
        let root = source("Features/App/AppRootView.swift")

        XCTAssertTrue(root.contains("SplashGating.shared"))
        XCTAssertTrue(root.contains("gating.shouldKeepSplash("))
        XCTAssertTrue(root.contains("isLoading: isListLoading"))
        XCTAssertTrue(root.contains("MIN_DURATION_MILLIS"))
    }

    func test_root_completesTheOnboardingThenNavigatesWithoutRestart() {
        let root = source("Features/App/AppRootView.swift")

        XCTAssertTrue(root.contains("onboardingViewModel.completeOnboarding()"))
        XCTAssertTrue(root.contains("showOnboarding = false"))
        XCTAssertTrue(root.contains("hasCompletedOnboarding()"))
    }

    func test_onboardingView_usesThePageTabViewStyleAndTheSharedNavigationRules() {
        let view = source("Features/Onboarding/OnboardingView.swift")

        XCTAssertTrue(view.contains(".tabViewStyle(.page"))
        XCTAssertTrue(view.contains("content.showsSkip("))
        XCTAssertTrue(view.contains("content.primaryButtonLabel("))
        XCTAssertTrue(view.contains("content.nextPageIndex("))
        XCTAssertTrue(view.contains("content.isLastPage("))
    }

    func test_splashView_usesTheBrandTealBackground() {
        XCTAssertTrue(source("Features/Splash/SplashView.swift").contains("0x0D6E68"))
    }
}
