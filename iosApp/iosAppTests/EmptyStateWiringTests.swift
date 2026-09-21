import XCTest
@testable import JobTracker

/// Garde-fous (lecture du source) : l'état « aucune candidature » a son icône « plateau vide » (tray) ; « aucun résultat de recherche » reste du texte seul.
final class EmptyStateWiringTests: XCTestCase {

    private let source: String = {
        let projectDir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let url = projectDir.appendingPathComponent("iosApp/Features/JobOffer/JobOfferListView.swift")
        return (try? String(contentsOf: url, encoding: .utf8)) ?? ""
    }()

    func test_emptyList_usesContentUnavailableViewWithTheTrayIcon() {
        XCTAssertFalse(source.isEmpty, "source introuvable (chemin #filePath)")
        XCTAssertEqual(source.components(separatedBy: "ContentUnavailableView {").count - 1, 1, "un seul état vide avec icône")
        XCTAssertTrue(source.contains("Image(systemName: \"tray\")"))
        XCTAssertTrue(source.contains("Text(\"Aucune candidature\")"))
        XCTAssertTrue(source.contains("Text(\"Ajoute ta première candidature avec le bouton +\")"), "le sous-titre doit être conservé")
    }

    func test_emptyStateIcon_isInTheEmptyBranchBeforeTheList() throws {
        let empty = try XCTUnwrap(source.range(of: "observable.state.offers.isEmpty"))
        let icon = try XCTUnwrap(source.range(of: "Image(systemName: \"tray\")"))
        let list = try XCTUnwrap(source.range(of: "List {"))

        XCTAssertTrue(empty.lowerBound < icon.lowerBound && icon.lowerBound < list.lowerBound,
                      "l'icône doit être dans la branche « liste vide », avant la List")
    }

    func test_searchWithNoResult_staysTextOnly() throws {
        let start = try XCTUnwrap(source.range(of: "Text(\"Aucun résultat pour"))
        // Le bloc « aucun résultat » : de son texte jusqu'au ForEach des cartes qui suit.
        let end = try XCTUnwrap(source.range(of: "ForEach(visibleOffers)", range: start.upperBound..<source.endIndex))
        let block = String(source[start.lowerBound..<end.lowerBound])

        XCTAssertFalse(block.contains("ContentUnavailableView"), "pas d'icône pour « aucun résultat de recherche »")
        XCTAssertFalse(block.contains("Image("))
    }

    func test_searchable_isStillAttachedToTheList() {
        XCTAssertTrue(source.contains(".searchable("), "la barre de recherche iOS ne doit pas être touchée")
        XCTAssertTrue(source.contains("placement: .navigationBarDrawer(displayMode: .always)"))
    }

    func test_emptyList_noLongerUsesTheLiteralBriefcase() {
        XCTAssertFalse(source.contains("systemName: \"briefcase\""), "l'icône de l'état vide n'est plus la mallette (choix UX : plateau vide)")
    }

    // MARK: action de tri (barre du haut)

    private var toolbarSource: String {
        String(source[(source.range(of: ".toolbar {")?.lowerBound ?? source.startIndex)...])
    }

    func test_sortMenu_isOnlyInTheToolbarWhenAtLeastOneOfferExists() throws {
        let toolbar = toolbarSource
        let condition = try XCTUnwrap(toolbar.range(of: "if !observable.state.offers.isEmpty {"), "condition absente de la toolbar")
        let sortIcon = try XCTUnwrap(toolbar.range(of: "Image(systemName: \"arrow.up.arrow.down\")"))

        XCTAssertTrue(sortIcon.lowerBound > condition.lowerBound, "le Menu de tri doit être DANS la condition")
        XCTAssertTrue(toolbar.range(of: "Menu {")!.lowerBound > condition.lowerBound)
    }

    func test_sortMenuCondition_usesAllOffersNotTheSearchFilteredOnes() throws {
        let toolbar = toolbarSource
        let end = try XCTUnwrap(toolbar.range(of: ".tint(Color.tealPrimary)"))
        let block = String(toolbar[..<end.lowerBound]).split(separator: "\n")
            .filter { !$0.trimmingCharacters(in: .whitespaces).hasPrefix("//") }   // les commentaires peuvent citer les noms interdits
            .joined(separator: "\n")

        XCTAssertFalse(block.contains("visibleOffers"), "la visibilité du tri ne doit pas dépendre du filtre de recherche")
        XCTAssertFalse(block.contains("searchQuery"))
    }

    func test_aboutButton_staysAlwaysVisibleOutsideTheSortCondition() throws {
        let toolbar = toolbarSource
        let about = try XCTUnwrap(toolbar.range(of: "AboutView()"))
        let condition = try XCTUnwrap(toolbar.range(of: "if !observable.state.offers.isEmpty {"))

        XCTAssertTrue(about.lowerBound < condition.lowerBound, "l'icône ⓘ doit rester hors de la condition")
    }
}
