import XCTest
import SharedLogic
@testable import JobLog

/// Pont Swift ↔ Kotlin : `KoinHelper` et `JobOfferListObservable` (observation du `StateFlow` du ViewModel partagé).
///
/// LIMITE ASSUMÉE : le constructeur du ViewModel est `internal` en Kotlin, on ne peut donc pas lui injecter un repository fake
/// depuis Swift. Ces tests utilisent le VRAI graphe Koin démarré par l'app hôte (`iOSApp.init`) et donc la vraie base de l'app
/// sur le simulateur ; chaque test travaille avec une offre au titre unique et la supprime à la fin.
@MainActor
final class KoinBridgeTests: XCTestCase {

    private var viewModels: [JobOfferListViewModel] = []

    override func tearDown() async throws {
        viewModels.forEach { $0.onCleared() }
        viewModels = []
    }

    private func makeViewModel() -> JobOfferListViewModel {
        let viewModel = KoinHelper().jobOfferListViewModel()
        viewModels.append(viewModel)
        return viewModel
    }

    /// Attend (par sondage) qu'une condition devienne vraie, sans dormir plus que nécessaire.
    private func waitUntil(timeout: TimeInterval = 10, _ condition: () -> Bool) async -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if condition() { return true }
            try? await Task.sleep(nanoseconds: 50_000_000)
        }
        return condition()
    }

    private func uniqueOffer() -> JobOffer {
        makeOffer(title: "XCTest-\(UUID().uuidString)", company: "PontKoin")
    }

    // MARK: KoinHelper

    func test_koinHelper_jobOfferListViewModel_returnsAViewModelFromTheRunningKoin() {
        XCTAssertNotNil(makeViewModel())
    }

    func test_koinHelper_jobOfferListViewModel_returnsANewInstanceEachTime() {
        let first = makeViewModel()
        let second = makeViewModel()

        XCTAssertFalse(first === second, "le module Koin déclare le ViewModel en factory")
    }

    func test_koinHelper_viewModel_startsWithLoadingStateThenLoads() async {
        let observable = JobOfferListObservable(viewModel: makeViewModel())

        let loaded = await waitUntil { !observable.state.isLoading }

        XCTAssertTrue(loaded, "le state doit finir par sortir de isLoading")
        XCTAssertNil(observable.state.errorMessage)
    }

    // MARK: JobOfferListObservable

    func test_observable_init_exposesTheViewModelsCurrentState() {
        let viewModel = makeViewModel()

        let observable = JobOfferListObservable(viewModel: viewModel)

        XCTAssertTrue(observable.viewModelRef === viewModel)
    }

    func test_observable_afterAddingAnOffer_publishesTheNewStateWithThatOffer() async {
        let viewModel = makeViewModel()
        let observable = JobOfferListObservable(viewModel: viewModel)
        _ = await waitUntil { !observable.state.isLoading }
        let offer = uniqueOffer()

        viewModel.onAddOffer(offer: offer)
        let appeared = await waitUntil { observable.state.offers.contains { $0.title == offer.title } }

        XCTAssertTrue(appeared, "l'offre ajoutée doit apparaître dans le state publié")
        // nettoyage
        if let saved = observable.state.offers.first(where: { $0.title == offer.title }) {
            viewModel.onDeleteOffer(offer: saved)
            _ = await waitUntil { !observable.state.offers.contains { $0.title == offer.title } }
        }
    }

    func test_observable_afterDeletingAnOffer_publishesTheStateWithoutIt() async {
        let viewModel = makeViewModel()
        let observable = JobOfferListObservable(viewModel: viewModel)
        _ = await waitUntil { !observable.state.isLoading }
        let offer = uniqueOffer()
        viewModel.onAddOffer(offer: offer)
        _ = await waitUntil { observable.state.offers.contains { $0.title == offer.title } }
        guard let saved = observable.state.offers.first(where: { $0.title == offer.title }) else {
            return XCTFail("offre ajoutée introuvable")
        }

        viewModel.onDeleteOffer(offer: saved)
        let gone = await waitUntil { !observable.state.offers.contains { $0.title == offer.title } }

        XCTAssertTrue(gone)
    }

    func test_observable_afterChangingStatus_publishesTheNewStatus() async {
        let viewModel = makeViewModel()
        let observable = JobOfferListObservable(viewModel: viewModel)
        _ = await waitUntil { !observable.state.isLoading }
        let offer = uniqueOffer()
        viewModel.onAddOffer(offer: offer)
        _ = await waitUntil { observable.state.offers.contains { $0.title == offer.title } }
        guard let saved = observable.state.offers.first(where: { $0.title == offer.title }) else {
            return XCTFail("offre ajoutée introuvable")
        }

        viewModel.onStatusChanged(offer: saved, newStatus: .interview)
        let changed = await waitUntil {
            observable.state.offers.first(where: { $0.title == offer.title })?.status == .interview
        }

        XCTAssertTrue(changed)
        viewModel.onDeleteOffer(offer: saved)
        _ = await waitUntil { !observable.state.offers.contains { $0.title == offer.title } }
    }

    func test_observable_afterValidationError_publishesTheErrorMessage() async {
        let viewModel = makeViewModel()
        let observable = JobOfferListObservable(viewModel: viewModel)
        _ = await waitUntil { !observable.state.isLoading }

        viewModel.onAddOffer(offer: makeOffer(title: "   ", company: "PontKoin"))
        let reported = await waitUntil { observable.state.errorMessage != nil }

        XCTAssertTrue(reported)
        XCTAssertEqual(observable.state.errorMessage, "The job title must not be blank")
    }

    func test_observable_twoObservablesOnTheSameViewModel_seeTheSameChange() async {
        let viewModel = makeViewModel()
        let first = JobOfferListObservable(viewModel: viewModel)
        let second = JobOfferListObservable(viewModel: viewModel)
        _ = await waitUntil { !first.state.isLoading && !second.state.isLoading }
        let offer = uniqueOffer()

        viewModel.onAddOffer(offer: offer)
        let both = await waitUntil {
            first.state.offers.contains { $0.title == offer.title } && second.state.offers.contains { $0.title == offer.title }
        }

        XCTAssertTrue(both)
        if let saved = first.state.offers.first(where: { $0.title == offer.title }) {
            viewModel.onDeleteOffer(offer: saved)
            _ = await waitUntil { !first.state.offers.contains { $0.title == offer.title } }
        }
    }
}
