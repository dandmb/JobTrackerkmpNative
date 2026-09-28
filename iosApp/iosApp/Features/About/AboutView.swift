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
                print("State observation error (About): \(error)")
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

    private let content = AboutContent.companion.of(language: AppLanguage.current)

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
        .navigationTitle(content.screenTitle)
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
        HStack(spacing: 16) {
            JobLogBrandTile(size: 72)   // logo de marque (parité avec la pastille de l'écran Android)
            VStack(alignment: .leading, spacing: 4) {
                Text(AboutContent.companion.APP_NAME)
                    .appTextStyle(.headlineSmall)
                    .foregroundStyle(Color.onAppBackground)
                Text(content.tagline)
                    .appTextStyle(.bodyLarge)
                    .foregroundStyle(Color.onSurfaceVariant)
                Text(AppVersion.label())
                    .appTextStyle(.labelLarge)
                    .foregroundStyle(Color.onSurfaceVariant)
            }
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
        let message = state.deletionFailed ? content.deleteAllFailedMessage : (state.dataDeleted ? content.deleteAllSuccessMessage : nil)
        return VStack(alignment: .leading, spacing: 12) {
            Text(content.deleteAllExplanation)
                .appTextStyle(.bodyMedium)
                .foregroundStyle(Color.onAppBackground)
            Button(role: .destructive) {
                observable.viewModel.onDeleteAllRequested()
            } label: {
                HStack(spacing: 8) {
                    if state.isDeleting { ProgressView() }
                    Text(state.isDeleting ? content.deleteAllInProgressLabel : content.deleteAllLabel)
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
            Text(content.contactTitle)
                .appTextStyle(.titleMedium)
                .foregroundStyle(Color.onAppBackground)
                .accessibilityAddTraits(.isHeader)
            Text(content.contactIntro).appTextStyle(.bodyMedium).foregroundStyle(Color.onAppBackground)
            Button(action: openMail) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(content.contactLabel)
                        .appTextStyle(.titleMedium)
                    Text(AboutContent.companion.CONTACT_EMAIL)
                        .appTextStyle(.bodyMedium)
                        .underline()
                }
                .foregroundStyle(Color.tealPrimary)
                .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityHint(content.contactNote)
            Text(content.contactNote).appTextStyle(.bodyMedium).foregroundStyle(Color.onSurfaceVariant)
            if noMailApp {
                Text(content.contactNoMailAppMessage)
                    .appTextStyle(.bodyMedium)
                    .foregroundStyle(Color.errorBase)
                    .accessibilityAddTraits(.updatesFrequently)
            }
        }
    }

    private func openMail() {
        guard let url = URL(string: AboutContent.companion.contactMailtoUri()) else {
            noMailApp = true
            return
        }
        openURL(url) { accepted in noMailApp = !accepted }
    }
}
