import SwiftUI
import SharedLogic

@MainActor
final class DocumentsFormState: ObservableObject {
    @Published var showImporter = false
    @Published var libraryChoice: AttachmentKind?
    @Published var opening: Attachment?
    @Published var message: String?
    @Published var importingKind: AttachmentKind?
    fileprivate var pendingImport: (kind: AttachmentKind, inLibrary: Bool)?

    fileprivate func startImport(kind: AttachmentKind, inLibrary: Bool) {
        pendingImport = (kind, inLibrary)
        message = nil
        showImporter = true
    }
}

struct DocumentsFormSection: View {
    @Binding var cvAttachmentId: Int64?
    @Binding var coverLetterAttachmentId: Int64?
    @ObservedObject var attachments: AttachmentsObservable
    @ObservedObject var state: DocumentsFormState

    var body: some View {
        Section {
            DocumentSlotRow(kind: .cv, label: L("form_document_cv"), attachmentId: $cvAttachmentId, attachments: attachments, state: state)
            DocumentSlotRow(kind: .coverLetter, label: L("form_document_letter"), attachmentId: $coverLetterAttachmentId, attachments: attachments, state: state)
        } header: {
            Text("form_section_documents")
        } footer: {
            if let message = state.message {
                Text(message)
                    .foregroundStyle(Color.errorBase)
                    .accessibilityAddTraits(.updatesFrequently)
            }
        }
    }
}

private struct DocumentSlotRow: View {
    let kind: AttachmentKind
    let label: String
    @Binding var attachmentId: Int64?
    @ObservedObject var attachments: AttachmentsObservable
    @ObservedObject var state: DocumentsFormState

    private var attachment: Attachment? { attachments.attachment(id: attachmentId) }
    private let language = AppLanguage.current

    var body: some View {
        HStack(spacing: 8) {
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .appTextStyle(.labelLarge)
                    .foregroundStyle(Color.onSurfaceVariant)
                Text(attachment?.displayName ?? L("form_document_none"))
                    .appTextStyle(.bodyLarge)
                if let attachment {
                    Text(details(attachment))
                        .appTextStyle(.bodyMedium)
                        .foregroundStyle(attachment.isMissing ? Color.errorBase : Color.onSurfaceVariant)
                }
                if state.importingKind == kind {
                    ProgressView().accessibilityLabel(L("documents_importing"))
                }
            }
            .accessibilityElement(children: .combine)
            Spacer(minLength: 0)
            if let attachment {
                if !attachment.isMissing {
                    Button {
                        state.opening = attachment
                    } label: {
                        Image(systemName: "eye")
                            .frame(minWidth: 44, minHeight: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.borderless)
                    .accessibilityLabel(L(attachment.format.isPreviewableInApp ? "documents_open" : "preview_open_with") + ", " + attachment.displayName)
                }
                Menu {
                    Menu(L("form_document_replace")) { importChoices }
                    Button(L("form_document_remove"), role: .destructive) {
                        state.message = nil
                        attachmentId = nil
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .frame(minWidth: 44, minHeight: 44)
                        .contentShape(Rectangle())
                }
                .accessibilityLabel(L("form_document_options_a11y", label))
            } else {
                Menu {
                    importChoices
                } label: {
                    Label(L("form_document_add"), systemImage: "plus")
                        .frame(minHeight: 44)
                }
                .accessibilityLabel(L("form_document_add_a11y", label))
            }
        }
        .tint(Color.tealPrimary)
        .disabled(state.importingKind != nil)
    }

    @ViewBuilder
    private var importChoices: some View {
        Button(L("form_document_choose_library")) { state.libraryChoice = kind }
        Button(L("form_document_import_library")) { state.startImport(kind: kind, inLibrary: true) }
        Button(L("form_document_import_once")) { state.startImport(kind: kind, inLibrary: false) }
    }

    private func details(_ attachment: Attachment) -> String {
        if attachment.isMissing { return AttachmentRules.shared.missingFileMessage(language: language) }
        let details = AttachmentFiles.details(attachment, language: language)
        return attachment.inLibrary ? details : "\(details) · \(L("form_document_one_time"))"
    }
}

private struct LibraryChoiceSheet: View {
    let kind: AttachmentKind
    @ObservedObject var attachments: AttachmentsObservable
    let onChosen: (Attachment) -> Void
    let onCancel: () -> Void

    var body: some View {
        let documents = attachments.state.library(kind: kind)
        NavigationStack {
            List {
                if documents.isEmpty {
                    Text(L("form_document_library_empty")).foregroundStyle(Color.onSurfaceVariant)
                }
                ForEach(documents, id: \.id) { document in
                    Button {
                        onChosen(document)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(document.displayName).appTextStyle(.bodyLarge).foregroundStyle(Color.onSurfaceBase)
                            Text(document.isMissing ? L("documents_missing") : AttachmentFiles.details(document))
                                .appTextStyle(.bodyMedium)
                                .foregroundStyle(document.isMissing ? Color.errorBase : Color.onSurfaceVariant)
                        }
                    }
                    .disabled(document.isMissing)
                }
            }
            .navigationTitle(L(kind == .cv ? "form_document_cv" : "form_document_letter"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L("cancel"), action: onCancel)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

private struct DocumentsFormPresentations: ViewModifier {
    @ObservedObject var state: DocumentsFormState
    @ObservedObject var attachments: AttachmentsObservable
    @Binding var cvAttachmentId: Int64?
    @Binding var coverLetterAttachmentId: Int64?

    func body(content: Content) -> some View {
        content
            .fileImporter(isPresented: $state.showImporter, allowedContentTypes: AttachmentFiles.contentTypes) { result in
                guard let pending = state.pendingImport, case .success(let url) = result else { return }
                state.pendingImport = nil
                Task {
                    state.importingKind = pending.kind
                    let outcome = await attachments.importFile(at: url, kind: pending.kind, inLibrary: pending.inLibrary)
                    state.importingKind = nil
                    if let imported = outcome as? AttachmentImportResult.Imported {
                        setId(imported.attachment.id, for: pending.kind)
                    } else if let rejected = outcome as? AttachmentImportResult.Rejected {
                        state.message = rejected.message
                    }
                }
            }
            .sheet(isPresented: Binding(get: { state.libraryChoice != nil }, set: { if !$0 { state.libraryChoice = nil } })) {
                if let kind = state.libraryChoice {
                    LibraryChoiceSheet(kind: kind, attachments: attachments) { chosen in
                        state.message = nil
                        setId(chosen.id, for: kind)
                        state.libraryChoice = nil
                    } onCancel: {
                        state.libraryChoice = nil
                    }
                }
            }
            .attachmentOpener($state.opening, attachments: attachments) { state.message = $0 }
    }

    private func setId(_ id: Int64, for kind: AttachmentKind) {
        if kind == .cv { cvAttachmentId = id } else { coverLetterAttachmentId = id }
    }
}

extension View {
    func documentsFormPresentations(
        state: DocumentsFormState,
        attachments: AttachmentsObservable,
        cvAttachmentId: Binding<Int64?>,
        coverLetterAttachmentId: Binding<Int64?>
    ) -> some View {
        modifier(DocumentsFormPresentations(
            state: state,
            attachments: attachments,
            cvAttachmentId: cvAttachmentId,
            coverLetterAttachmentId: coverLetterAttachmentId
        ))
    }
}
