import XCTest
import SharedLogic
@testable import JobTracker

final class LocalDateBridgeTests: XCTestCase {

    private let calendar = Calendar(identifier: .gregorian)

    private func kotlinDate(_ y: Int32, _ m: Int32, _ d: Int32) -> Kotlinx_datetimeLocalDate {
        Kotlinx_datetimeLocalDate(year: y, monthNumber: m, dayOfMonth: d)
    }

    private func components(of date: Date) -> DateComponents {
        calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
    }

    // MARK: Kotlin → Swift

    func test_toDate_regularDate_hasTheSameYearMonthDay() {
        let c = components(of: kotlinDate(2026, 9, 5).toDate())

        XCTAssertEqual(c.year, 2026)
        XCTAssertEqual(c.month, 9)
        XCTAssertEqual(c.day, 5)
    }

    func test_toDate_isMidnightLocalTime() {
        let c = components(of: kotlinDate(2026, 9, 5).toDate())

        XCTAssertEqual(c.hour, 0)
        XCTAssertEqual(c.minute, 0)
        XCTAssertEqual(c.second, 0)
    }

    func test_toDate_leapDay_isFebruary29() {
        let c = components(of: kotlinDate(2028, 2, 29).toDate())

        XCTAssertEqual(c.month, 2)
        XCTAssertEqual(c.day, 29)
    }

    func test_toDate_yearBoundaries_areKept() {
        XCTAssertEqual(components(of: kotlinDate(2026, 1, 1).toDate()).day, 1)
        let end = components(of: kotlinDate(2026, 12, 31).toDate())
        XCTAssertEqual(end.month, 12)
        XCTAssertEqual(end.day, 31)
    }

    func test_toDate_orderOfDates_isPreserved() {
        XCTAssertLessThan(kotlinDate(2026, 8, 20).toDate(), kotlinDate(2026, 9, 1).toDate())
        XCTAssertLessThan(kotlinDate(2026, 12, 31).toDate(), kotlinDate(2027, 1, 1).toDate())
    }

    // MARK: Swift → Kotlin

    func test_toKotlinLocalDate_regularDate_hasTheSameYearMonthDay() {
        let date = calendar.date(from: DateComponents(year: 2026, month: 9, day: 5, hour: 15, minute: 30))!

        let kotlin = date.toKotlinLocalDate()

        XCTAssertEqual(kotlin.year, 2026)
        XCTAssertEqual(kotlin.monthNumber, 9)
        XCTAssertEqual(kotlin.dayOfMonth, 5)
    }

    func test_toKotlinLocalDate_timeOfDay_isIgnored() {
        let morning = calendar.date(from: DateComponents(year: 2026, month: 9, day: 5, hour: 0, minute: 1))!
        let night = calendar.date(from: DateComponents(year: 2026, month: 9, day: 5, hour: 23, minute: 59))!

        XCTAssertEqual(morning.toKotlinLocalDate(), night.toKotlinLocalDate())
    }

    func test_toKotlinLocalDate_leapDay_isFebruary29() {
        let date = calendar.date(from: DateComponents(year: 2028, month: 2, day: 29))!

        let kotlin = date.toKotlinLocalDate()

        XCTAssertEqual(kotlin.monthNumber, 2)
        XCTAssertEqual(kotlin.dayOfMonth, 29)
    }

    // MARK: aller-retour

    func test_roundTrip_kotlinToSwiftToKotlin_isLossless() {
        for (y, m, d) in [(2026, 9, 5), (2028, 2, 29), (2026, 12, 31), (2026, 1, 1), (1999, 7, 14)] {
            let original = kotlinDate(Int32(y), Int32(m), Int32(d))

            XCTAssertEqual(original.toDate().toKotlinLocalDate(), original, "\(y)-\(m)-\(d)")
        }
    }

    func test_roundTrip_swiftToKotlinToSwift_keepsTheDayAtMidnight() {
        let date = calendar.date(from: DateComponents(year: 2026, month: 9, day: 5, hour: 18))!

        let back = date.toKotlinLocalDate().toDate()

        XCTAssertEqual(back, calendar.startOfDay(for: date))
    }
}
