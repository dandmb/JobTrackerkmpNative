import XCTest
import UniformTypeIdentifiers
import SharedLogic
@testable import JobLog

final class AttachmentFilesTests: XCTestCase {

    private let fileManager = FileManager.default
    private var workDirectory: URL!

    override func setUpWithError() throws {
        workDirectory = fileManager.temporaryDirectory.appendingPathComponent("attachment-files-tests-\(UUID().uuidString)")
        try fileManager.createDirectory(at: workDirectory, withIntermediateDirectories: true)
    }

    override func tearDownWithError() throws {
        try? fileManager.removeItem(at: workDirectory)
        try? fileManager.removeItem(atPath: DirectoryAttachmentFileStoreKt.attachmentShareDirectoryPath())
    }

    private func attachment(name: String, format: AttachmentFormat = .pdf) -> Attachment {
        Attachment(
            id: 1, displayName: name, kind: .cv, format: format, sizeBytes: 3, addedAtEpochMillis: 0,
            inLibrary: true, storageName: "stored.\(format.`extension`)", usageCount: 0, isMissing: false
        )
    }

    func test_picker_offersExactlyTheFiveSupportedFormats() {
        XCTAssertEqual(
            Set(AttachmentFiles.contentTypes.map(\.identifier)),
            ["com.adobe.pdf", "com.microsoft.word.doc", "org.openxmlformats.wordprocessingml.document", "public.jpeg", "public.png"]
        )
    }

    func test_copyUpToLimit_copiesASmallFileEntirely() throws {
        let source = workDirectory.appendingPathComponent("small.pdf")
        try Data("%PDF-1.7".utf8).write(to: source)
        let target = workDirectory.appendingPathComponent("copy")

        let size = AttachmentFiles.copyUpToLimit(from: source, to: target, limit: 100)

        XCTAssertEqual(size, 8)
        XCTAssertEqual(try Data(contentsOf: target), Data("%PDF-1.7".utf8))
    }

    func test_copyUpToLimit_countsTheWholeFileButNeverWritesBeyondTheLimit() throws {
        let source = workDirectory.appendingPathComponent("big.pdf")
        try Data(repeating: 7, count: 200 * 1024).write(to: source)
        let target = workDirectory.appendingPathComponent("copy")

        let size = AttachmentFiles.copyUpToLimit(from: source, to: target, limit: 100 * 1024)

        XCTAssertEqual(size, 200 * 1024, "la taille réelle sert au message « trop gros »")
        XCTAssertLessThanOrEqual(try Data(contentsOf: target).count, 100 * 1024)
    }

    func test_copyUpToLimit_unreadableSource_returnsNil() {
        let size = AttachmentFiles.copyUpToLimit(
            from: workDirectory.appendingPathComponent("missing.pdf"),
            to: workDirectory.appendingPathComponent("copy"),
            limit: 100
        )

        XCTAssertNil(size)
    }

    func test_shareFileName_usesTheDisplayNameWithoutForbiddenCharacters() {
        XCTAssertEqual(AttachmentFiles.shareFileName(attachment(name: "CV / Data: 2026?")), "CV _ Data_ 2026_.pdf")
        XCTAssertEqual(AttachmentFiles.shareFileName(attachment(name: "   ", format: .docx)), "document.docx")
        XCTAssertEqual(AttachmentFiles.shareFileName(attachment(name: String(repeating: "a", count: 120))).count, 80 + 4)
    }

    func test_presentableCopy_isNamedAfterTheDocumentInTheShareDirectory_andReplacesThePreviousOne() throws {
        let stored = workDirectory.appendingPathComponent("3402bf17.pdf")
        try Data("%PDF".utf8).write(to: stored)

        let first = try XCTUnwrap(AttachmentFiles.presentableCopy(of: attachment(name: "Ancien"), storedPath: stored.path))
        let second = try XCTUnwrap(AttachmentFiles.presentableCopy(of: attachment(name: "CV Data - FR"), storedPath: stored.path))

        XCTAssertEqual(second.lastPathComponent, "CV Data - FR.pdf")
        XCTAssertEqual(second.deletingLastPathComponent().standardizedFileURL.path,
                       URL(fileURLWithPath: DirectoryAttachmentFileStoreKt.attachmentShareDirectoryPath()).standardizedFileURL.path)
        XCTAssertFalse(fileManager.fileExists(atPath: first.path), "une seule copie à la fois")
        XCTAssertEqual(try Data(contentsOf: second), Data("%PDF".utf8))
    }

    func test_presentableCopy_missingStoredFile_returnsNil() {
        XCTAssertNil(AttachmentFiles.presentableCopy(of: attachment(name: "CV"), storedPath: workDirectory.appendingPathComponent("gone.pdf").path))
    }
}
