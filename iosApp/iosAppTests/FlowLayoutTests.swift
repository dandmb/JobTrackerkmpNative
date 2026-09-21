import XCTest
@testable import JobLog

/// `FlowLayout.arrange` est un calcul pur (tailles → positions) : testable sans rendu SwiftUI.
final class FlowLayoutTests: XCTestCase {

    private func box(_ w: CGFloat, _ h: CGFloat = 20) -> CGSize { CGSize(width: w, height: h) }

    func test_arrange_noViews_isEmptyAndZeroSized() {
        let result = FlowLayout.arrange(sizes: [], maxWidth: 300, spacing: 8)

        XCTAssertEqual(result.origins, [])
        XCTAssertEqual(result.size, .zero)
    }

    func test_arrange_singleView_isPlacedAtOrigin() {
        let result = FlowLayout.arrange(sizes: [box(100, 30)], maxWidth: 300, spacing: 8)

        XCTAssertEqual(result.origins, [CGPoint(x: 0, y: 0)])
        XCTAssertEqual(result.size, CGSize(width: 100, height: 30))
    }

    func test_arrange_viewsThatFitOnOneRow_areSeparatedByTheSpacing() {
        let result = FlowLayout.arrange(sizes: [box(80), box(60), box(50)], maxWidth: 300, spacing: 8)

        XCTAssertEqual(result.origins, [CGPoint(x: 0, y: 0), CGPoint(x: 88, y: 0), CGPoint(x: 156, y: 0)])
        XCTAssertEqual(result.size, CGSize(width: 206, height: 20))
    }

    func test_arrange_viewThatDoesNotFit_wrapsToTheNextRow() {
        let result = FlowLayout.arrange(sizes: [box(150), box(100), box(100)], maxWidth: 300, spacing: 8)

        // 150 + 8 + 100 = 258 tient ; le 3e (258 + 8 + 100 > 300) passe à la ligne suivante.
        XCTAssertEqual(result.origins, [CGPoint(x: 0, y: 0), CGPoint(x: 158, y: 0), CGPoint(x: 0, y: 28)])
        XCTAssertEqual(result.size.height, 48)
    }

    func test_arrange_rowSpacingEqualsHorizontalSpacing() {
        let result = FlowLayout.arrange(sizes: [box(200), box(200)], maxWidth: 250, spacing: 12)

        XCTAssertEqual(result.origins[1], CGPoint(x: 0, y: 32))   // 20 (hauteur) + 12 (espacement)
    }

    func test_arrange_exactFit_staysOnTheSameRow() {
        // 100 + 8 + 92 = 200 = maxWidth : « ne tient plus » n'est vrai qu'au-delà, pas à égalité.
        let result = FlowLayout.arrange(sizes: [box(100), box(92)], maxWidth: 200, spacing: 8)

        XCTAssertEqual(result.origins[1], CGPoint(x: 108, y: 0))
    }

    func test_arrange_oneUnitOverMaxWidth_wraps() {
        let result = FlowLayout.arrange(sizes: [box(100), box(93)], maxWidth: 200, spacing: 8)

        XCTAssertEqual(result.origins[1], CGPoint(x: 0, y: 28))
    }

    func test_arrange_viewWiderThanMaxWidth_isPlacedAloneOnItsRowWithoutBlankRowBefore() {
        let result = FlowLayout.arrange(sizes: [box(400)], maxWidth: 300, spacing: 8)

        XCTAssertEqual(result.origins, [CGPoint(x: 0, y: 0)])
        XCTAssertEqual(result.size.width, 400)
    }

    func test_arrange_tooWideViewInTheMiddle_getsItsOwnRow() {
        let result = FlowLayout.arrange(sizes: [box(50), box(400), box(50)], maxWidth: 300, spacing: 8)

        XCTAssertEqual(result.origins, [CGPoint(x: 0, y: 0), CGPoint(x: 0, y: 28), CGPoint(x: 0, y: 56)])
    }

    func test_arrange_rowHeight_isTheTallestViewOfThatRow() {
        let result = FlowLayout.arrange(sizes: [box(100, 20), box(100, 50), box(250, 10)], maxWidth: 250, spacing: 8)

        // Ligne 1 : hauteur 50 ; ligne 2 démarre à 50 + 8.
        XCTAssertEqual(result.origins[2], CGPoint(x: 0, y: 58))
        XCTAssertEqual(result.size.height, 68)
    }

    func test_arrange_totalWidth_isTheWidestRowWithoutTrailingSpacing() {
        let result = FlowLayout.arrange(sizes: [box(100), box(100), box(60)], maxWidth: 220, spacing: 10)

        // Ligne 1 : 100 + 10 + 100 = 210 ; ligne 2 : 60. Largeur = 210 (pas 220).
        XCTAssertEqual(result.size.width, 210)
    }

    func test_arrange_infiniteMaxWidth_neverWraps() {
        let result = FlowLayout.arrange(sizes: Array(repeating: box(100), count: 10), maxWidth: .infinity, spacing: 8)

        XCTAssertTrue(result.origins.allSatisfy { $0.y == 0 })
        XCTAssertEqual(result.size.width, 10 * 100 + 9 * 8)
    }

    func test_arrange_zeroSpacing_placesViewsBackToBack() {
        let result = FlowLayout.arrange(sizes: [box(50), box(50)], maxWidth: 200, spacing: 0)

        XCTAssertEqual(result.origins, [CGPoint(x: 0, y: 0), CGPoint(x: 50, y: 0)])
    }

    func test_arrange_originsCount_matchesInputCount() {
        let sizes = Array(repeating: box(70), count: 7)

        XCTAssertEqual(FlowLayout.arrange(sizes: sizes, maxWidth: 200, spacing: 8).origins.count, 7)
    }

    func test_flowLayout_defaultSpacing_isEight() {
        XCTAssertEqual(FlowLayout().spacing, 8)
    }
}
