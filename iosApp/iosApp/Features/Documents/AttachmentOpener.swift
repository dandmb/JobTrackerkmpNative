import SwiftUI
import QuickLook
import SharedLogic

private struct SharedFile: Identifiable {
    let url: URL
    var id: URL { url }
}

private struct ActivitySheet: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

private final class DocumentPreviewController: QLPreviewController, QLPreviewControllerDataSource, QLPreviewControllerDelegate {
    private let url: URL

    init(url: URL) {
        self.url = url
        super.init(nibName: nil, bundle: nil)
        dataSource = self
        delegate = self
    }

    required init?(coder: NSCoder) { nil }

    func numberOfPreviewItems(in controller: QLPreviewController) -> Int { 1 }

    func previewController(_ controller: QLPreviewController, previewItemAt index: Int) -> QLPreviewItem { url as NSURL }

    // L'aperçu porte sur une copie temporaire : des annotations y seraient perdues sans avertissement.
    func previewController(_ controller: QLPreviewController, editingModeFor previewItem: QLPreviewItem) -> QLPreviewItemEditingMode {
        .disabled
    }

    @MainActor
    static func present(url: URL) {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first { $0.activationState == .foregroundActive }
        guard var top = scene?.keyWindow?.rootViewController else { return }
        while let presented = top.presentedViewController { top = presented }
        top.present(DocumentPreviewController(url: url), animated: true)
    }
}

private struct AttachmentOpenerModifier: ViewModifier {
    @Binding var request: Attachment?
    let attachments: AttachmentsObservable
    let onFailure: (String) -> Void

    @State private var sharedFile: SharedFile?

    func body(content: Content) -> some View {
        content
            .sheet(item: $sharedFile) { file in
                ActivitySheet(url: file.url)
                    .presentationDetents([.medium, .large])
            }
            .onChange(of: request?.id) { _, id in
                guard id != nil, let attachment = request else { return }
                request = nil
                open(attachment)
            }
    }

    private func open(_ attachment: Attachment) {
        guard let url = AttachmentFiles.presentableCopy(of: attachment, storedPath: attachments.viewModel.pathOf(attachment: attachment)) else {
            onFailure(L("preview_failed"))
            return
        }
        if attachment.format.isPreviewableInApp {
            DocumentPreviewController.present(url: url)
        } else {
            sharedFile = SharedFile(url: url)
        }
    }
}

extension View {
    func attachmentOpener(_ request: Binding<Attachment?>, attachments: AttachmentsObservable, onFailure: @escaping (String) -> Void) -> some View {
        modifier(AttachmentOpenerModifier(request: request, attachments: attachments, onFailure: onFailure))
    }
}
