import XCTest
@testable import JobLog

final class CardAndFormLabelWiringTests: XCTestCase {

    private static let projectDir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()

    private func read(_ relative: String) -> String {
        (try? String(contentsOf: Self.projectDir.appendingPathComponent(relative), encoding: .utf8)) ?? ""
    }

    func test_editButtonLabel_namesTheOfferAsTheCardDisplaysIt() {
        let card = read("iosApp/Features/JobOffer/JobOfferCard.swift")
        XCTAssertTrue(card.contains(".accessibilityLabel(L(\"card_edit_a11y\", offer.title.toTitleCase()))"))
    }

    func test_documentAddButton_iconHasTheBrandColourLikeItsText() {
        let section = read("iosApp/Features/Documents/DocumentsFormSection.swift")
        XCTAssertTrue(section.contains("Label(L(\"form_document_add\"), systemImage: \"plus\")\n                        .foregroundStyle(Color.tealPrimary)"),
                      "la teinte de la ligne n'atteint pas l'icône d'un Menu : couleur explicite")
    }
}
