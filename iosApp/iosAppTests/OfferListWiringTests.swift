import XCTest
import SharedLogic
@testable import JobLog

final class OfferListWiringTests: XCTestCase {

    private let listView: String = {
        let dir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let url = dir.appendingPathComponent("iosApp/Features/JobOffer/JobOfferListView.swift")
        return (try? String(contentsOf: url, encoding: .utf8)) ?? ""
    }()

    private let logicBridge: String = {
        let dir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let url = dir.appendingPathComponent("iosApp/Features/JobOffer/JobOfferListLogic.swift")
        return (try? String(contentsOf: url, encoding: .utf8)) ?? ""
    }()

    private let filterRow: String = {
        let dir = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let url = dir.appendingPathComponent("iosApp/Features/JobOffer/StatusFilterRow.swift")
        return (try? String(contentsOf: url, encoding: .utf8)) ?? ""
    }()

    // MARK: la vue appelle la fonction partagée, ne la réimplémente pas

    func test_listView_computesVisibleOffers_byCallingTheSharedFilterFunction() {
        XCTAssertFalse(listView.isEmpty, "source introuvable (chemin #filePath)")
        XCTAssertTrue(listView.contains(".filteredForDisplay(query: searchQuery, sortOption: sortOption, selectedStatuses: selectedStatuses)"))
    }

    func test_logicBridge_delegatesToOfferListFilter_doesNotReimplementSortingItself() {
        XCTAssertFalse(logicBridge.isEmpty)
        XCTAssertTrue(logicBridge.contains("OfferListFilter.shared.apply("), "doit déléguer à l'objet Kotlin partagé")
        XCTAssertFalse(logicBridge.contains(".sorted {"), "le tri ne doit plus être réimplémenté en Swift")
        XCTAssertFalse(logicBridge.contains("localizedCaseInsensitiveContains"), "la recherche ne doit plus être réimplémentée en Swift")
    }

    // MARK: rangée de puces : ScrollView horizontal, pas de retour à la ligne

    func test_statusFilterRow_scrollsHorizontally() {
        XCTAssertFalse(filterRow.isEmpty)
        XCTAssertTrue(filterRow.contains("ScrollView(.horizontal"))
    }

    func test_statusFilterRow_hasOneChipPerStatus_plusAnAllChip() {
        XCTAssertTrue(filterRow.contains("L(\"filter_all\")"))
        XCTAssertTrue(filterRow.contains("ForEach(ApplicationStatus.entries, id: \\.self)"))
    }

    func test_statusFilterRow_selectedChipColor_matchesTheStatusColor() {
        let chipBlock = filterRow.components(separatedBy: "ForEach(ApplicationStatus.entries").last ?? ""
        XCTAssertTrue(chipBlock.contains("tint: status.color"))
    }

    // MARK: sémantique « Tous » / multi-sélection (mêmes règles que sharedLogic : ensemble vide = aucun filtre)

    func test_allChip_selectsAnEmptySet() {
        let allChipBlock = filterRow.components(separatedBy: "ForEach(ApplicationStatus.entries").first ?? ""
        XCTAssertTrue(allChipBlock.contains("selectedStatuses = []"))
        XCTAssertTrue(allChipBlock.contains("isSelected: selectedStatuses.isEmpty"))
    }

    func test_statusChip_toggling_addsOrRemovesFromTheSelection_neverReplacesIt() {
        let chipBlock = filterRow.components(separatedBy: "ForEach(ApplicationStatus.entries").last ?? ""
        XCTAssertTrue(chipBlock.contains("selectedStatuses.remove(status)"))
        XCTAssertTrue(chipBlock.contains("selectedStatuses.insert(status)"))
    }

    // MARK: rangée insérée avant la carte de statistiques, dans la même List que la recherche

    func test_statusFilterRow_isInsertedBeforeTheStatsCard() {
        guard let filterRowUse = listView.range(of: "StatusFilterRow(selectedStatuses:"),
              let statsCard = listView.range(of: "JobOfferStatsCard(offers:") else {
            return XCTFail("StatusFilterRow ou JobOfferStatsCard introuvable dans JobOfferListView.swift")
        }
        XCTAssertTrue(filterRowUse.lowerBound < statsCard.lowerBound, "la rangée de filtre doit précéder la carte de statistiques")
    }

    func test_selectedStatuses_isResetWhenTheOfferListBecomesEmpty() {
        let onChangeBlock = listView.components(separatedBy: "onChange(of: observable.state.offers.isEmpty)").last ?? ""
        XCTAssertTrue(onChangeBlock.contains("selectedStatuses = []"))
    }

    // MARK: état vide spécifique au filtre (priorité documentée : filtre avant recherche, comme Android)

    func test_emptyFilterResult_showsADedicatedMessage_priorOverTheSearchMessage() {
        XCTAssertTrue(listView.contains("L(\"list_no_results_filter\")"))
        XCTAssertTrue(listView.contains("selectedStatuses.isEmpty ? L(\"list_no_results\", searchQuery) : L(\"list_no_results_filter\")"))
    }

    // MARK: l'action de tri reste soumise à state.offers, pas à la sélection filtrée (règle inchangée)

    func test_sortAction_staysGatedOnAllOffers_notOnTheFilteredSelection() {
        let toolbar = listView.components(separatedBy: ".toolbar {").last ?? ""
        XCTAssertTrue(toolbar.contains("if !observable.state.offers.isEmpty {"))
        XCTAssertFalse(toolbar.contains("if !visibleOffers.isEmpty {"))
    }

    // MARK: pont Swift → Kotlin réellement exercé (pas seulement lu dans le source)

    func test_filteredForDisplay_bridgesToTheSharedObject_sameResultAsCallingItDirectly() {
        let offers = [
            makeOffer(id: 1, title: "Android dev", status: .interview),
            makeOffer(id: 2, title: "Android dev", status: .rejected),
        ]

        let viaExtension = offers.filteredForDisplay(query: "android", sortOption: .dateDesc, selectedStatuses: [.interview])
        let direct = OfferListFilter.shared.apply(offers: offers, query: "android", sortOption: SortOption.dateDesc, selectedStatuses: [.interview])

        XCTAssertEqual(viaExtension.map { $0.id }, [1])
        XCTAssertEqual(viaExtension.map { $0.id }, direct.map { $0.id })
    }

    func test_filteredForDisplay_defaultSelectedStatuses_isEmptySet_noFilter() {
        let offers = [makeOffer(id: 1, status: .pending), makeOffer(id: 2, status: .accepted)]

        XCTAssertEqual(Set(offers.filteredForDisplay(query: "", sortOption: .dateDesc).map { $0.id }), [1, 2])
    }
}
