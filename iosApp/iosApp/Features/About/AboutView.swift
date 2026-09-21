//
//  AboutView.swift
//  iosApp
//

import SwiftUI
import SharedLogic
import KMPNativeCoroutinesAsync

/// Observe `AboutViewModel` (même principe que `JobOfferListObservable`) et annule son scope à la disparition de l'écran.
@MainActor
final class AboutObservable: ObservableObject {
    let viewModel: AboutViewModel
    @Published private(set) var state: AboutState
    private var task: Task<Void, Never>?

    init(viewModel: AboutViewModel = KoinHelper().aboutViewModel()) {
        self.viewModel = viewModel
        self.state = viewModel.state
        task = Task { [weak self] in
            do {
                for try await newState in asyncSequence(for: viewModel.stateFlow) {
                    self?.state = newState
                }
            } catch {
                print("Erreur d'observation du state (À propos): \(error)")
            }
        }
    }

    deinit {
        task?.cancel()
        viewModel.onCleared()
    }
}

/// Écran « À propos ». Tout le texte vient de `AboutContent` (sharedLogic) : cet écran ne fait que le mettre en forme.
/// La suppression totale passe par la double confirmation portée par `AboutViewModel`
/// (1re étape : `confirmationDialog` ; étape finale : `alert`).
struct AboutView: View {
    @StateObject private var observable = AboutObservable()
    /// L'alerte finale est présentée avec un léger délai : présenter un 2e dialogue pendant la fermeture du 1er est ignoré par UIKit.
    @State private var showFinalAlert = false
    @State private var noMailApp = false
    @Environment(\.openURL) private var openURL

    private let content = AboutContent.shared

    private var step: DeleteAllStep { observable.state.deleteStep }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                header
                ForEach(Array(content.sections.enumerated()), id: \.offset) { _, section in
                    sectionView(section)
                }
                deleteAllBlock
                contactBlock
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Color.appBackground.ignoresSafeArea())
        .navigationTitle(content.SCREEN_TITLE)
        .navigationBarTitleDisplayMode(.inline)
        .confirmationDialog(
            content.firstConfirmation.title,
            isPresented: Binding(
                get: { step == .firstConfirmation },
                // Fermeture sans bouton (tap à côté) = annulation ; après « Continuer » l'étape a déjà changé : rien à annuler.
                set: { if !$0 && step == .firstConfirmation { observable.viewModel.onDeleteAllCancelled() } }
            ),
            titleVisibility: .visible
        ) {
            Button(content.firstConfirmation.confirmLabel, role: .destructive) {
                observable.viewModel.onDeleteAllFirstConfirmed()
            }
            Button(content.firstConfirmation.cancelLabel, role: .cancel) {
                observable.viewModel.onDeleteAllCancelled()
            }
        } message: {
            Text(content.firstConfirmation.message)
        }
        .alert(content.finalConfirmation.title, isPresented: $showFinalAlert) {
            Button(content.finalConfirmation.confirmLabel, role: .destructive) {
                observable.viewModel.onDeleteAllFinalConfirmed()
            }
            Button(content.finalConfirmation.cancelLabel, role: .cancel) {
                observable.viewModel.onDeleteAllCancelled()
            }
        } message: {
            Text(content.finalConfirmation.message)
        }
        .onChange(of: step) { _, newStep in
            if newStep == .finalConfirmation {
                Task {
                    try? await Task.sleep(nanoseconds: 400_000_000)
                    if step == .finalConfirmation { showFinalAlert = true }
                }
            } else {
                showFinalAlert = false
            }
        }
    }

    // MARK: - Blocs

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(content.APP_NAME)
                .appTextStyle(.headlineSmall)
                .foregroundStyle(Color.onAppBackground)
            Text(content.TAGLINE)
                .appTextStyle(.bodyLarge)
                .foregroundStyle(Color.onSurfaceVariant)
            Text(AppVersion.label())
                .appTextStyle(.labelLarge)
                .foregroundStyle(Color.onSurfaceVariant)
        }
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

    /// Zone d'alerte : fond teinté d'erreur, bouton destructif — clairement distincte du reste de l'écran.
    private var deleteAllBlock: some View {
        let state = observable.state
        let message = state.errorMessage ?? (state.dataDeleted ? content.DELETE_ALL_SUCCESS_MESSAGE : nil)
        return VStack(alignment: .leading, spacing: 12) {
            Text(content.DELETE_ALL_EXPLANATION)
                .appTextStyle(.bodyMedium)
                .foregroundStyle(Color.onAppBackground)
            Button(role: .destructive) {
                observable.viewModel.onDeleteAllRequested()
            } label: {
                HStack(spacing: 8) {
                    if state.isDeleting { ProgressView() }
                    Text(state.isDeleting ? content.DELETE_ALL_IN_PROGRESS_LABEL : content.DELETE_ALL_LABEL)
                        .appTextStyle(.labelLarge.copy(weight: .semiBold, size: 16))
                }
                .frame(maxWidth: .infinity)
                .frame(minHeight: 32)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.errorBase)
            .disabled(state.isDeleting)
            if let message {
                Text(message)
                    .appTextStyle(.bodyMedium)
                    .foregroundStyle(Color.onAppBackground)
                    .accessibilityAddTraits(.updatesFrequently)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.errorBase.opacity(0.14), in: RoundedRectangle(cornerRadius: 16))
    }

    /// Lien de contact : ouvre l'application de messagerie (lien mailto + objet pré-rempli, définis dans sharedLogic).
    /// Si aucune application ne peut l'ouvrir, l'adresse s'affiche en clair.
    private var contactBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(content.CONTACT_TITLE)
                .appTextStyle(.titleMedium)
                .foregroundStyle(Color.onAppBackground)
                .accessibilityAddTraits(.isHeader)
            Text(content.CONTACT_INTRO).appTextStyle(.bodyMedium).foregroundStyle(Color.onAppBackground)
            Button(action: openMail) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(content.CONTACT_LABEL)
                        .appTextStyle(.titleMedium)
                    Text(content.CONTACT_EMAIL)
                        .appTextStyle(.bodyMedium)
                        .underline()
                }
                .foregroundStyle(Color.tealPrimary)
                .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityHint(content.CONTACT_NOTE)
            Text(content.CONTACT_NOTE).appTextStyle(.bodyMedium).foregroundStyle(Color.onSurfaceVariant)
            if noMailApp {
                Text(content.CONTACT_NO_MAIL_APP_MESSAGE)
                    .appTextStyle(.bodyMedium)
                    .foregroundStyle(Color.errorBase)
                    .accessibilityAddTraits(.updatesFrequently)
            }
        }
    }

    private func openMail() {
        guard let url = URL(string: content.contactMailtoUri()) else {
            noMailApp = true
            return
        }
        openURL(url) { accepted in noMailApp = !accepted }
    }
}
