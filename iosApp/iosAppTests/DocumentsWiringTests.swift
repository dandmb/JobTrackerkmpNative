import XCTest
@testable import JobLog

final class DocumentsWiringTests: XCTestCase {

    private static let projectDir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()

    private func read(_ relative: String) -> String {
        (try? String(contentsOf: Self.projectDir.appendingPathComponent(relative), encoding: .utf8)) ?? ""
    }

    private lazy var settings = read("iosApp/Features/Settings/SettingsView.swift")
    private lazy var documents = read("iosApp/Features/Documents/DocumentsView.swift")
    private lazy var formSection = read("iosApp/Features/Documents/DocumentsFormSection.swift")
    private lazy var files = read("iosApp/Features/Documents/AttachmentFiles.swift")
    private lazy var opener = read("iosApp/Features/Documents/AttachmentOpener.swift")
    private lazy var form = read("iosApp/Features/JobOffer/JobOfferFormSheet.swift")
    private lazy var card = read("iosApp/Features/JobOffer/JobOfferCard.swift")
    private lazy var infoPlist = read("iosApp/Info.plist")

    func test_sourcesAreReadable() {
        for source in [settings, documents, formSection, files, opener, form, card, infoPlist] {
            XCTAssertFalse(source.isEmpty, "source introuvable (chemin #filePath)")
        }
    }

    func test_documentsScreen_isTheFirstSettingsEntry() throws {
        let documentsLink = try XCTUnwrap(settings.range(of: "DocumentsView()"))
        let aboutLink = try XCTUnwrap(settings.range(of: "AboutView()"))

        XCTAssertTrue(documentsLink.lowerBound < aboutLink.lowerBound)
        XCTAssertTrue(settings.contains("content.documentsRowLabel"))
    }

    func test_import_usesTheSystemFilePicker_andNeverKeepsTheOriginalURL() {
        XCTAssertTrue(documents.contains(".fileImporter(isPresented: $showImporter, allowedContentTypes: AttachmentFiles.contentTypes)"))
        XCTAssertTrue(formSection.contains(".fileImporter(isPresented: $state.showImporter, allowedContentTypes: AttachmentFiles.contentTypes)"))
        XCTAssertTrue(files.contains("url.startAccessingSecurityScopedResource()"))
        XCTAssertTrue(files.contains("url.stopAccessingSecurityScopedResource()"))
        XCTAssertTrue(files.contains("viewModel.importCopiedFile("), "le ViewModel reçoit une copie locale")
        XCTAssertTrue(files.contains("rules.MAX_SIZE_BYTES"), "taille contrôlée avant et pendant la copie")
        for forbidden in ["bookmarkData", "absoluteString"] {
            XCTAssertFalse(files.contains(forbidden), "« \(forbidden) » : l'URL d'origine ne doit pas être conservée")
        }
    }

    func test_noNewPermission_andNoPhotoLibraryImport() {
        for key in ["NSPhotoLibraryUsageDescription", "NSPhotoLibraryAddUsageDescription", "NSCameraUsageDescription"] {
            XCTAssertFalse(infoPlist.contains(key), "\(key) : l'import passe uniquement par le sélecteur de fichiers (D6)")
        }
        XCTAssertFalse(formSection.contains("PhotosPicker"))
        XCTAssertFalse(documents.contains("PhotosPicker"))
    }

    func test_opening_previewsWithQuickLook_andSharesTheOtherFormats() {
        XCTAssertTrue(opener.contains("DocumentPreviewController.present(url: url)"))
        XCTAssertTrue(opener.contains("editingModeFor previewItem: QLPreviewItem) -> QLPreviewItemEditingMode {\n        .disabled"),
                      "pas d'annotation : l'aperçu porte sur une copie temporaire")
        XCTAssertTrue(opener.contains("UIActivityViewController(activityItems: [url]"))
        XCTAssertTrue(opener.contains("if attachment.format.isPreviewableInApp {"))
        XCTAssertTrue(files.contains("DirectoryAttachmentFileStoreKt.attachmentShareDirectoryPath()"),
                      "la copie présentée doit être dans le dossier effacé par la suppression totale")
    }

    func test_formSection_offersTheThreeImportChoices_andSavesTheIds() throws {
        for key in ["form_document_choose_library", "form_document_import_library", "form_document_import_once",
                    "form_document_replace", "form_document_remove"] {
            XCTAssertTrue(formSection.contains("L(\"\(key)\")"), key)
        }
        XCTAssertTrue(form.contains("cvAttachmentId: cvAttachmentId,\n            coverLetterAttachmentId: coverLetterAttachmentId"))
        XCTAssertTrue(form.contains("existingOffer?.cvAttachmentId?.int64Value"))
        let section = try XCTUnwrap(form.range(of: "DocumentsFormSection("))
        let notes = try XCTUnwrap(form.range(of: "Section(\"form_field_notes\")"))
        XCTAssertTrue(section.lowerBound < notes.lowerBound, "section Documents avant les notes, comme sur Android")
    }

    func test_libraryChoice_cannotAttachAFileMissingFromTheDevice() {
        XCTAssertTrue(formSection.contains(".disabled(document.isMissing)"))
    }

    func test_missingFile_isShownInTheLibraryAndInTheForm() {
        XCTAssertTrue(documents.contains("AttachmentRules.shared.missingFileMessage(language: language)"))
        XCTAssertTrue(formSection.contains("AttachmentRules.shared.missingFileMessage(language: language)"))
        XCTAssertTrue(formSection.contains("if !attachment.isMissing {"), "pas d'aperçu pour un fichier introuvable")
    }

    func test_deletionStillInUse_showsTheSharedMessage() {
        XCTAssertTrue(documents.contains("as? AttachmentDeletionResult.StillInUse"))
        XCTAssertTrue(documents.contains("AttachmentRules.shared.stillInUseMessage(usageCount:"))
    }

    func test_card_showsThePaperclipOnlyWhenADocumentIsAttached() {
        XCTAssertTrue(card.contains("Text(\"📎 \" + documentLabels.joined(separator: \" · \"))"))
        XCTAssertTrue(card.contains("if !documentLabels.isEmpty {"))
    }

    func test_newButtons_haveAccessibilityLabels() {
        XCTAssertTrue(documents.contains(".accessibilityLabel(L(\"documents_actions_a11y\", document.displayName))"))
        XCTAssertTrue(documents.contains(".accessibilityLabel(L(\"documents_importing\"))"))
        XCTAssertTrue(documents.contains("L(\"documents_missing\")"))
        XCTAssertTrue(formSection.contains(".accessibilityLabel(L(\"form_document_add_a11y\", label))"))
        XCTAssertTrue(formSection.contains(".accessibilityLabel(L(\"form_document_options_a11y\", label))"))
        XCTAssertTrue(formSection.contains("+ \", \" + attachment.displayName)"), "le bouton Aperçu nomme le document")
    }

    func test_renameAndDeletionErrors_areShown_notSwallowed() {
        let observable = read("iosApp/Features/Documents/AttachmentsObservable.swift")
        XCTAssertFalse(observable.contains("try?"), "une erreur ne doit plus être avalée en silence")
        XCTAssertEqual(documents.components(separatedBy: "AttachmentRules.shared.actionFailedMessage(language: language)").count - 1, 2,
                       "renommage et suppression")
    }
}
