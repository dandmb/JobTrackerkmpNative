import SwiftUI
import SharedLogic

struct DocumentsView: View {
    @StateObject private var attachments = AttachmentsObservable()
    @State private var importKind: AttachmentKind?
    @State private var showImporter = false
    @State private var importing = false
    @State private var message: String?
    @State private var renaming: Attachment?
    @State private var newName = ""
    @State private var deleting: Attachment?
    @State private var opening: Attachment?

    private let content = SettingsContent.companion.of(language: AppLanguage.current)
    private let language = AppLanguage.current

    var body: some View {
        List {
            documentSection(kind: .cv, title: L("documents_section_cv"), addLabel: L("documents_add_cv"), emptyLabel: L("documents_empty_cv"))
            documentSection(
                kind: .coverLetter,
                title: L("documents_section_letters"),
                addLabel: L("documents_add_letter"),
                emptyLabel: L("documents_empty_letters")
            )
            if attachments.state.librarySizeBytes > 0 {
                Section {
                } footer: {
                    Text(L("documents_total_size", AttachmentRules.shared.sizeLabel(sizeBytes: attachments.state.librarySizeBytes, language: language)))
                        .appTextStyle(.bodyMedium)
                }
            }
        }
        .overlay(alignment: .top) {
            if importing {
                ProgressView()
                    .padding(12)
                    .background(.regularMaterial, in: Capsule())
                    .accessibilityLabel(L("documents_importing"))
            }
        }
        .navigationTitle(content.documentsRowLabel)
        .navigationBarTitleDisplayMode(.inline)
        .fileImporter(isPresented: $showImporter, allowedContentTypes: AttachmentFiles.contentTypes) { result in
            guard let kind = importKind, case .success(let url) = result else { return }
            importKind = nil
            Task {
                importing = true
                let outcome = await attachments.importFile(at: url, kind: kind, inLibrary: true)
                importing = false
                if let rejected = outcome as? AttachmentImportResult.Rejected { message = rejected.message }
            }
        }
        .alert(L("documents_rename_title"), isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField(L("documents_name_label"), text: $newName)
            Button(L("documents_rename")) {
                if let attachment = renaming {
                    let name = newName
                    Task {
                        do {
                            try await attachments.rename(attachment, to: name)
                        } catch {
                            message = AttachmentRules.shared.actionFailedMessage(language: language)
                        }
                    }
                }
                renaming = nil
            }
            .disabled(newName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            Button(L("cancel"), role: .cancel) { renaming = nil }
        }
        .alert(
            L("documents_delete_confirm_title"),
            isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }),
            presenting: deleting
        ) { attachment in
            Button(L("delete_action"), role: .destructive) {
                Task {
                    do {
                        let result = try await attachments.deleteFromLibrary(attachment)
                        if let inUse = result as? AttachmentDeletionResult.StillInUse {
                            message = AttachmentRules.shared.stillInUseMessage(usageCount: inUse.usageCount, language: language)
                        }
                    } catch {
                        message = AttachmentRules.shared.actionFailedMessage(language: language)
                    }
                }
            }
            Button(L("cancel"), role: .cancel) {}
        } message: { attachment in
            Text(L("documents_delete_confirm_message", attachment.displayName))
        }
        .alert(message ?? "", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } })) {
            Button(L("ok"), role: .cancel) {}
        }
        .attachmentOpener($opening, attachments: attachments) { message = $0 }
    }

    private func documentSection(kind: AttachmentKind, title: String, addLabel: String, emptyLabel: String) -> some View {
        let documents = attachments.state.library(kind: kind)
        return Section {
            if documents.isEmpty {
                Text(emptyLabel)
                    .appTextStyle(.bodyMedium)
                    .foregroundStyle(Color.onSurfaceVariant)
            }
            ForEach(documents, id: \.id) { document in
                row(document)
            }
        } header: {
            HStack {
                Text(title)
                Spacer()
                Button {
                    importKind = kind
                    showImporter = true
                } label: {
                    Label(addLabel, systemImage: "plus")
                }
                .disabled(importing)
            }
            .textCase(nil)
        }
    }

    private func row(_ document: Attachment) -> some View {
        HStack(spacing: 12) {
            if document.isMissing {
                summary(document)
            } else {
                Button {
                    opening = document
                } label: {
                    summary(document)
                }
                .buttonStyle(.plain)
            }

            Menu {
                Button(L(document.format.isPreviewableInApp ? "documents_open" : "preview_open_with")) { opening = document }
                    .disabled(document.isMissing)
                Button(L("documents_rename")) {
                    newName = document.displayName
                    renaming = document
                }
                Button(L("delete_action"), role: .destructive) { deleting = document }
            } label: {
                Image(systemName: "ellipsis.circle")
                    .frame(minWidth: 44, minHeight: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(L("documents_actions_a11y", document.displayName))
        }
    }

    private func summary(_ document: Attachment) -> some View {
        HStack(spacing: 12) {
            Image(systemName: document.isMissing ? "exclamationmark.circle" : "doc.text")
                .foregroundStyle(document.isMissing ? Color.errorBase : Color.onSurfaceVariant)
                .accessibilityLabel(document.isMissing ? L("documents_missing") : "")
                .accessibilityHidden(!document.isMissing)
            VStack(alignment: .leading, spacing: 2) {
                Text(document.displayName)
                    .appTextStyle(.bodyLarge)
                    .foregroundStyle(Color.onSurfaceBase)
                Text(document.isMissing ? AttachmentRules.shared.missingFileMessage(language: language) : detailsWithUsage(document))
                    .appTextStyle(.bodyMedium)
                    .foregroundStyle(document.isMissing ? Color.errorBase : Color.onSurfaceVariant)
            }
            Spacer(minLength: 0)
        }
        .contentShape(Rectangle())
    }

    private func detailsWithUsage(_ document: Attachment) -> String {
        let usage: String
        switch document.usageCount {
        case 0: usage = L("documents_unused")
        case 1: usage = L("documents_used_by_one", Int(document.usageCount))
        default: usage = L("documents_used_by_other", Int(document.usageCount))
        }
        return "\(AttachmentFiles.details(document, language: language)) · \(usage)"
    }
}
