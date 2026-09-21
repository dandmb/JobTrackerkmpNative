import XCTest
@testable import JobTracker

/// Le format de date de la carte doit rester identique à Android (`DateFormattingTest.kt`) : « 5 sept. », sans zéro initial.
final class CardDateFormatTests: XCTestCase {

    private func format(_ year: Int, _ month: Int, _ day: Int) -> String {
        let date = Calendar(identifier: .gregorian).date(from: DateComponents(year: year, month: month, day: day))!
        return JobOfferCard.dateFormatter.string(from: date)
    }

    func test_dateFormatter_everyMonth_usesTheFrenchAbbreviation() {
        let expected = [
            "5 janv.", "5 févr.", "5 mars", "5 avr.", "5 mai", "5 juin",
            "5 juil.", "5 août", "5 sept.", "5 oct.", "5 nov.", "5 déc.",
        ]

        for (index, text) in expected.enumerated() {
            XCTAssertEqual(format(2026, index + 1, 5), text, "mois \(index + 1)")
        }
    }

    func test_dateFormatter_singleDigitDay_hasNoLeadingZero() {
        XCTAssertEqual(format(2026, 3, 1), "1 mars")
        XCTAssertEqual(format(2026, 7, 9), "9 juil.")
    }

    func test_dateFormatter_twoDigitDay_isPrintedInFull() {
        XCTAssertEqual(format(2026, 10, 10), "10 oct.")
        XCTAssertEqual(format(2026, 12, 31), "31 déc.")
    }

    func test_dateFormatter_leapDay_isFebruary29() {
        XCTAssertEqual(format(2028, 2, 29), "29 févr.")
    }

    func test_dateFormatter_year_isNotPrinted() {
        XCTAssertEqual(format(1999, 9, 5), format(2026, 9, 5))
    }

    func test_dateFormatter_isPinnedToFrenchLocale_whateverTheDeviceLanguage() {
        XCTAssertEqual(JobOfferCard.dateFormatter.locale.identifier, "fr_FR")
    }
}
