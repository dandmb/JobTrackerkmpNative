import XCTest
import SharedLogic
@testable import JobLog

/// Le format de date de la carte vient de sharedLogic (`ShortDate`) : identique à Android (`DateFormattingTest.kt`),
/// « 5 sept. » en français, « Sep 5 » en anglais, sans zéro initial.
final class CardDateFormatTests: XCTestCase {

    private func format(_ year: Int32, _ month: Int32, _ day: Int32, _ language: AppLanguage) -> String {
        JobOfferCard.shortDate(Kotlinx_datetimeLocalDate(year: year, monthNumber: month, dayOfMonth: day), language: language)
    }

    func test_shortDate_french_everyMonth_usesTheFrenchAbbreviation() {
        let expected = [
            "5 janv.", "5 févr.", "5 mars", "5 avr.", "5 mai", "5 juin",
            "5 juil.", "5 août", "5 sept.", "5 oct.", "5 nov.", "5 déc.",
        ]

        for (index, text) in expected.enumerated() {
            XCTAssertEqual(format(2026, Int32(index + 1), 5, AppLanguage.fr), text, "mois \(index + 1)")
        }
    }

    func test_shortDate_english_everyMonth_isMonthThenDay() {
        let expected = ["Jan 5", "Feb 5", "Mar 5", "Apr 5", "May 5", "Jun 5", "Jul 5", "Aug 5", "Sep 5", "Oct 5", "Nov 5", "Dec 5"]

        for (index, text) in expected.enumerated() {
            XCTAssertEqual(format(2026, Int32(index + 1), 5, AppLanguage.en), text, "month \(index + 1)")
        }
    }

    func test_shortDate_singleAndTwoDigitDays() {
        XCTAssertEqual(format(2026, 3, 1, AppLanguage.fr), "1 mars")
        XCTAssertEqual(format(2026, 3, 1, AppLanguage.en), "Mar 1")
        XCTAssertEqual(format(2026, 12, 31, AppLanguage.fr), "31 déc.")
        XCTAssertEqual(format(2028, 2, 29, AppLanguage.en), "Feb 29")
    }

    func test_shortDate_year_isNotPrinted() {
        XCTAssertEqual(format(1999, 9, 5, AppLanguage.fr), format(2026, 9, 5, AppLanguage.fr))
    }

    func test_shortDate_doesNotDependOnTheDeviceLocale_onlyOnTheRequestedLanguage() {
        XCTAssertNotEqual(format(2026, 9, 5, AppLanguage.fr), format(2026, 9, 5, AppLanguage.en))
    }
}
