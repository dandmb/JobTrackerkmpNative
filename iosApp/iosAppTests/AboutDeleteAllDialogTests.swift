import XCTest
import SharedLogic
@testable import JobLog

/// NB : `onDeleteAllFinalConfirmed()` n'est volontairement JAMAIS appelé : il viderait la vraie base de l'app hôte sur le simulateur.
@MainActor
final class AboutDeleteAllDialogTests: XCTestCase {

    private func waitUntil(_ condition: () -> Bool, timeout: TimeInterval = 2) async {
        let deadline = Date().addingTimeInterval(timeout)
        while !condition() && Date() < deadline {
            try? await Task.sleep(nanoseconds: 10_000_000)
        }
    }

    func test_firstConfirmationDismissedAfterContinue_keepsTheFinalConfirmation() async {
        let viewModel = KoinHelper().aboutViewModel()
        let observable = AboutObservable(viewModel: viewModel)
        viewModel.onDeleteAllRequested()
        await waitUntil { observable.state.deleteStep == .firstConfirmation }
        XCTAssertEqual(observable.state.deleteStep, .firstConfirmation)

        // Ordre observé sous iOS 26 : l'action du bouton « Continue » s'exécute, puis la fermeture du dialogue
        // appelle le setter de isPresented, avant que la copie Swift de l'état ne soit rafraîchie.
        viewModel.onDeleteAllFirstConfirmed()
        observable.firstConfirmationDismissed()

        XCTAssertEqual(viewModel.state.deleteStep, .finalConfirmation, "la fermeture du dialogue ne doit pas annuler une confirmation déjà reçue")
        viewModel.onDeleteAllCancelled()
    }

    func test_firstConfirmationDismissedWithoutAnswer_cancelsTheSequence() async {
        let viewModel = KoinHelper().aboutViewModel()
        let observable = AboutObservable(viewModel: viewModel)
        viewModel.onDeleteAllRequested()
        await waitUntil { observable.state.deleteStep == .firstConfirmation }

        observable.firstConfirmationDismissed()

        XCTAssertEqual(viewModel.state.deleteStep, .idle)
    }

    func test_firstConfirmationDismissedAfterCancel_staysIdle() async {
        let viewModel = KoinHelper().aboutViewModel()
        let observable = AboutObservable(viewModel: viewModel)
        viewModel.onDeleteAllRequested()
        await waitUntil { observable.state.deleteStep == .firstConfirmation }

        viewModel.onDeleteAllCancelled()
        observable.firstConfirmationDismissed()

        XCTAssertEqual(viewModel.state.deleteStep, .idle)
    }
}
