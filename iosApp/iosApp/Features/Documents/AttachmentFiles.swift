import Foundation
import UniformTypeIdentifiers
import SharedLogic
import KMPNativeCoroutinesAsync

enum AttachmentFiles {

    static let contentTypes: [UTType] = [
        .pdf,
        UTType("com.microsoft.word.doc"),
        UTType("org.openxmlformats.wordprocessingml.document"),
        .jpeg,
        .png,
    ].compactMap { $0 }

    static func importPickedFile(
        _ url: URL,
        kind: AttachmentKind,
        inLibrary: Bool,
        viewModel: AttachmentsViewModel,
        language: AppLanguage
    ) async -> AttachmentImportResult {
        let rules = AttachmentRules.shared
        let accessing = url.startAccessingSecurityScopedResource()
        defer { if accessing { url.stopAccessingSecurityScopedResource() } }

        let values = try? url.resourceValues(forKeys: [.fileSizeKey, .nameKey, .contentTypeKey])
        if let size = values?.fileSize, Int64(size) > rules.MAX_SIZE_BYTES {
            return AttachmentImportResult.Rejected(message: rules.tooLargeMessage(sizeBytes: Int64(size), language: language))
        }
        let temporaryPath = viewModel.doNewTemporaryPath()
        let limit = rules.MAX_SIZE_BYTES
        let copiedSize = await Task.detached(priority: .userInitiated) {
            copyUpToLimit(from: url, to: URL(fileURLWithPath: temporaryPath), limit: limit)
        }.value

        guard let copiedSize, copiedSize <= limit else {
            try? FileManager.default.removeItem(atPath: temporaryPath)
            let message = copiedSize.map { rules.tooLargeMessage(sizeBytes: $0, language: language) } ?? rules.importFailedMessage(language: language)
            return AttachmentImportResult.Rejected(message: message)
        }
        do {
            return try await asyncFunction(for: viewModel.importCopiedFile(
                temporaryPath: temporaryPath,
                originalFileName: values?.name ?? url.lastPathComponent,
                mimeType: values?.contentType?.preferredMIMEType,
                kind: kind,
                inLibrary: inLibrary,
                language: language
            ))
        } catch {
            try? FileManager.default.removeItem(atPath: temporaryPath)
            return AttachmentImportResult.Rejected(message: rules.importFailedMessage(language: language))
        }
    }

    static func copyUpToLimit(from source: URL, to target: URL, limit: Int64) -> Int64? {
        guard FileManager.default.createFile(atPath: target.path, contents: nil),
              let input = try? FileHandle(forReadingFrom: source),
              let output = try? FileHandle(forWritingTo: target) else { return nil }
        defer {
            try? input.close()
            try? output.close()
        }
        var total: Int64 = 0
        do {
            while let chunk = try input.read(upToCount: 64 * 1024), !chunk.isEmpty {
                if total + Int64(chunk.count) <= limit { try output.write(contentsOf: chunk) }
                total += Int64(chunk.count)
            }
        } catch {
            return nil
        }
        return total
    }

    static func shareFileName(_ attachment: Attachment) -> String {
        let forbidden = CharacterSet(charactersIn: "\\/:*?\"<>|").union(.controlCharacters)
        let cleaned = String(attachment.displayName.unicodeScalars.map { forbidden.contains($0) ? "_" : Character($0) })
            .trimmingCharacters(in: .whitespaces)
        let base = cleaned.isEmpty ? "document" : String(cleaned.prefix(80))
        return "\(base).\(attachment.format.`extension`)"
    }

    static func presentableCopy(of attachment: Attachment, storedPath: String) -> URL? {
        let fileManager = FileManager.default
        let directory = URL(fileURLWithPath: DirectoryAttachmentFileStoreKt.attachmentShareDirectoryPath(), isDirectory: true)
        try? fileManager.removeItem(at: directory)
        let target = directory.appendingPathComponent(shareFileName(attachment))
        do {
            try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
            try fileManager.copyItem(at: URL(fileURLWithPath: storedPath), to: target)
            return target
        } catch {
            return nil
        }
    }

    static func details(_ attachment: Attachment, language: AppLanguage = .current) -> String {
        "\(attachment.format.name) · \(AttachmentRules.shared.sizeLabel(sizeBytes: attachment.sizeBytes, language: language))"
    }
}
