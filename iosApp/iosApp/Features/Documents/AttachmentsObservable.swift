import SwiftUI
import SharedLogic
import KMPNativeCoroutinesAsync

@MainActor
final class AttachmentsObservable: ObservableObject {
    let viewModel: AttachmentsViewModel
    @Published private(set) var state: AttachmentsState
    private var task: Task<Void, Never>?

    init(viewModel: AttachmentsViewModel = KoinHelper().attachmentsViewModel()) {
        self.viewModel = viewModel
        self.state = viewModel.state
        task = Task { [weak self] in
            do {
                for try await newState in asyncSequence(for: viewModel.stateFlow) {
                    self?.state = newState
                }
            } catch {
                print("State observation error (Attachments): \(error)")
            }
        }
    }

    deinit {
        task?.cancel()
        viewModel.onCleared()
    }

    func attachment(id: Int64?) -> Attachment? {
        state.byId(id: id.map { KotlinLong(value: $0) })
    }

    func importFile(at url: URL, kind: AttachmentKind, inLibrary: Bool) async -> AttachmentImportResult {
        await AttachmentFiles.importPickedFile(url, kind: kind, inLibrary: inLibrary, viewModel: viewModel, language: .current)
    }

    func rename(_ attachment: Attachment, to name: String) async throws {
        _ = try await asyncFunction(for: viewModel.rename(attachmentId: attachment.id, displayName: name))
    }

    func deleteFromLibrary(_ attachment: Attachment) async throws -> AttachmentDeletionResult {
        try await asyncFunction(for: viewModel.deleteFromLibrary(attachmentId: attachment.id))
    }
}
