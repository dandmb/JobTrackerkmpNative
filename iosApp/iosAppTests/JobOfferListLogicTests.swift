import XCTest
import SharedLogic
@testable import JobTracker

/// Miroir de `OfferListLogicTest.kt` (Android). Les écarts volontairement documentés sont marqués « currently ».
final class JobOfferListLogicTests: XCTestCase {

    private let android = makeOffer(id: 1, title: "Développeur Android", company: "Spotify", applied: (2026, 9, 5))
    private let ios = makeOffer(id: 2, title: "iOS Engineer", company: "eBay", applied: (2026, 8, 20))
    private let backend = makeOffer(id: 3, title: "backend dev", company: "Zeta", applied: (2026, 9, 12))
    private var all: [JobOffer] { [android, ios, backend] }

    private func ids(_ offers: [JobOffer]) -> [Int64] { offers.map { $0.id } }

    // MARK: filtrage

    func test_searchedAndSorted_emptyQuery_keepsEveryOffer() {
        XCTAssertEqual(Set(ids(all.searchedAndSorted(query: "", sortOption: .dateDesc))), [1, 2, 3])
    }

    func test_searchedAndSorted_emptyList_returnsEmptyList() {
        XCTAssertTrue([JobOffer]().searchedAndSorted(query: "x", sortOption: .alphaAsc).isEmpty)
    }

    func test_searchedAndSorted_queryMatchingATitle_returnsOnlyThatOffer() {
        XCTAssertEqual(ids(all.searchedAndSorted(query: "android", sortOption: .dateDesc)), [1])
    }

    func test_searchedAndSorted_queryMatchingACompany_returnsOnlyThatOffer() {
        XCTAssertEqual(ids(all.searchedAndSorted(query: "zeta", sortOption: .dateDesc)), [3])
    }

    func test_searchedAndSorted_query_isCaseInsensitive() {
        XCTAssertEqual(ids(all.searchedAndSorted(query: "IOS ENGINEER", sortOption: .dateDesc)), [2])
        XCTAssertEqual(ids(all.searchedAndSorted(query: "ebay", sortOption: .dateDesc)), [2])
    }

    func test_searchedAndSorted_query_matchesPartOfAWord() {
        XCTAssertEqual(ids(all.searchedAndSorted(query: "dévelop", sortOption: .dateDesc)), [1])
    }

    func test_searchedAndSorted_queryWithoutAccents_doesNotMatchAccentedTitle() {
        // Caractérisation : ni iOS (localizedCaseInsensitiveContains) ni Android (contains ignoreCase) ne plient les accents.
        XCTAssertTrue(all.searchedAndSorted(query: "develop", sortOption: .dateDesc).isEmpty)
    }

    func test_searchedAndSorted_queryMatchingNothing_returnsEmptyList() {
        XCTAssertTrue(all.searchedAndSorted(query: "cobol", sortOption: .dateDesc).isEmpty)
    }

    func test_searchedAndSorted_doesNotSearchInOtherFieldsLikeLocation() {
        let offer = makeOffer(id: 9, title: "Dev", company: "Acme", location: "Paris")

        XCTAssertTrue([offer].searchedAndSorted(query: "paris", sortOption: .dateDesc).isEmpty)
    }

    func test_searchedAndSorted_whitespaceOnlyQuery_isTreatedAsNoFilterLikeAndroid() {
        // Parité avec Android (`query.isBlank()`) : une recherche d'espaces ne filtre rien (avant : liste vide sur iOS).
        XCTAssertEqual(all.searchedAndSorted(query: "   ", sortOption: .dateDesc).count, 3)
        XCTAssertEqual(all.searchedAndSorted(query: "\t", sortOption: .dateDesc).count, 3)
        XCTAssertEqual(all.searchedAndSorted(query: " \n ", sortOption: .dateDesc).count, 3)
    }

    func test_searchedAndSorted_queryWithSurroundingSpaces_isUsedAsIsWithoutTrimming() {
        // Comme Android (caractérisation) : seul un query 100 % blanc est ignoré ; « dev  » n'est pas nettoyé.
        XCTAssertTrue(all.searchedAndSorted(query: "dev  ", sortOption: .dateDesc).isEmpty)
        XCTAssertEqual(ids(all.searchedAndSorted(query: "end dev", sortOption: .dateDesc)), [3])
    }

    // MARK: tri

    func test_searchedAndSorted_dateDesc_putsTheMostRecentApplicationFirst() {
        XCTAssertEqual(ids(all.searchedAndSorted(query: "", sortOption: .dateDesc)), [3, 1, 2])
    }

    func test_searchedAndSorted_dateAsc_putsTheOldestApplicationFirst() {
        XCTAssertEqual(ids(all.searchedAndSorted(query: "", sortOption: .dateAsc)), [2, 1, 3])
    }

    func test_searchedAndSorted_alphaAsc_sortsByTitleIgnoringCase() {
        let offers = [makeOffer(id: 1, title: "banana"), makeOffer(id: 2, title: "Apple"), makeOffer(id: 3, title: "cherry")]

        XCTAssertEqual(ids(offers.searchedAndSorted(query: "", sortOption: .alphaAsc)), [2, 1, 3])
    }

    func test_searchedAndSorted_alphaDesc_sortsByTitleIgnoringCaseInReverse() {
        let offers = [makeOffer(id: 1, title: "banana"), makeOffer(id: 2, title: "Apple"), makeOffer(id: 3, title: "cherry")]

        XCTAssertEqual(ids(offers.searchedAndSorted(query: "", sortOption: .alphaDesc)), [3, 1, 2])
    }

    func test_searchedAndSorted_dateTies_keepTheOriginalOrder() {
        let offers = (1...5).map { makeOffer(id: Int64($0), title: "t\($0)", applied: (2026, 9, 1)) }

        XCTAssertEqual(ids(offers.searchedAndSorted(query: "", sortOption: .dateDesc)), [1, 2, 3, 4, 5])
        XCTAssertEqual(ids(offers.searchedAndSorted(query: "", sortOption: .dateAsc)), [1, 2, 3, 4, 5])
    }

    func test_searchedAndSorted_titlesEqualIgnoringCase_keepTheOriginalOrder() {
        let offers = [makeOffer(id: 1, title: "dev"), makeOffer(id: 2, title: "Dev"), makeOffer(id: 3, title: "DEV")]

        XCTAssertEqual(ids(offers.searchedAndSorted(query: "", sortOption: .alphaAsc)), [1, 2, 3])
        XCTAssertEqual(ids(offers.searchedAndSorted(query: "", sortOption: .alphaDesc)), [1, 2, 3])
    }

    func test_searchedAndSorted_alphaSortWithAccentedTitle_ordersByUnicodeNotByLocale() {
        // Limite connue (identique à Android) : « École » passe après « Zoé » en tri A → Z.
        let offers = [makeOffer(id: 1, title: "École"), makeOffer(id: 2, title: "Zoé"), makeOffer(id: 3, title: "Alpha")]

        XCTAssertEqual(ids(offers.searchedAndSorted(query: "", sortOption: .alphaAsc)), [3, 2, 1])
    }

    // MARK: combinaison

    func test_searchedAndSorted_filterThenSort_appliesBothInOrder() {
        let offers = [
            makeOffer(id: 1, title: "Dev C", applied: (2026, 9, 1)),
            makeOffer(id: 2, title: "Dev A", applied: (2026, 9, 3)),
            makeOffer(id: 3, title: "Chef", applied: (2026, 9, 2)),
            makeOffer(id: 4, title: "Dev B", applied: (2026, 9, 2)),
        ]

        XCTAssertEqual(ids(offers.searchedAndSorted(query: "dev", sortOption: .alphaAsc)), [2, 4, 1])
        XCTAssertEqual(ids(offers.searchedAndSorted(query: "dev", sortOption: .dateAsc)), [1, 4, 2])
    }

    func test_searchedAndSorted_doesNotMutateTheInputArray() {
        let input = [backend, android, ios]

        _ = input.searchedAndSorted(query: "", sortOption: .alphaAsc)

        XCTAssertEqual(ids(input), [3, 1, 2])
    }
}
