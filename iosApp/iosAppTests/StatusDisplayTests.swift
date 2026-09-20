import XCTest
import SwiftUI
import UIKit
import SharedLogic
@testable import JobTracker

final class StatusDisplayTests: XCTestCase {

    // MARK: libellés (miroir de StatusLabelsTest.kt)

    func test_displayLabel_everyStatus_returnsItsFrenchLabel() {
        XCTAssertEqual(ApplicationStatus.pending.displayLabel, "En attente")
        XCTAssertEqual(ApplicationStatus.applied.displayLabel, "Postulé")
        XCTAssertEqual(ApplicationStatus.interview.displayLabel, "Entretien")
        XCTAssertEqual(ApplicationStatus.rejected.displayLabel, "Refusé")
        XCTAssertEqual(ApplicationStatus.accepted.displayLabel, "Accepté")
    }

    func test_shortLabel_everyStatus_returnsItsLowercaseShortLabel() {
        XCTAssertEqual(ApplicationStatus.pending.shortLabel, "attente")
        XCTAssertEqual(ApplicationStatus.applied.shortLabel, "postulé")
        XCTAssertEqual(ApplicationStatus.interview.shortLabel, "entretien")
        XCTAssertEqual(ApplicationStatus.rejected.shortLabel, "refusé")
        XCTAssertEqual(ApplicationStatus.accepted.shortLabel, "accepté")
    }

    func test_displayLabels_coverEveryStatusAndAreDistinct() {
        let labels = ApplicationStatus.entries.map { $0.displayLabel }

        XCTAssertEqual(ApplicationStatus.entries.count, 5)
        XCTAssertEqual(Set(labels).count, 5)
        XCTAssertFalse(labels.contains("?"), "un statut retombe sur le cas par défaut")
    }

    // MARK: options de tri

    func test_sortOption_rawValues_areTheMenuLabelsSameAsAndroid() {
        XCTAssertEqual(SortOption.dateDesc.rawValue, "Plus récent")
        XCTAssertEqual(SortOption.dateAsc.rawValue, "Plus ancien")
        XCTAssertEqual(SortOption.alphaAsc.rawValue, "A → Z")
        XCTAssertEqual(SortOption.alphaDesc.rawValue, "Z → A")
        XCTAssertEqual(SortOption.allCases.count, 4)
    }

    func test_sortOption_id_isItsRawValue() {
        XCTAssertEqual(SortOption.dateDesc.id, "Plus récent")
    }

    // MARK: couleurs de statut — garde-fou WCAG AA (miroir de StatusColorsTest.kt)

    private func rgb(_ color: Color, dark: Bool) -> (Double, Double, Double) {
        let traits = UITraitCollection(userInterfaceStyle: dark ? .dark : .light)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        UIColor(color).resolvedColor(with: traits).getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Double(r), Double(g), Double(b))
    }

    private func luminance(_ c: (Double, Double, Double)) -> Double {
        func lin(_ v: Double) -> Double { v <= 0.03928 ? v / 12.92 : pow((v + 0.055) / 1.055, 2.4) }
        return 0.2126 * lin(c.0) + 0.7152 * lin(c.1) + 0.0722 * lin(c.2)
    }

    private func contrast(_ a: (Double, Double, Double), _ b: (Double, Double, Double)) -> Double {
        let la = luminance(a), lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private func blend(_ fg: (Double, Double, Double), over bg: (Double, Double, Double), alpha: Double) -> (Double, Double, Double) {
        (fg.0 * alpha + bg.0 * (1 - alpha), fg.1 * alpha + bg.1 * (1 - alpha), fg.2 * alpha + bg.2 * (1 - alpha))
    }

    func test_statusColors_reachAaContrastOnCardAndStatsBadge_inBothModes() {
        for dark in [false, true] {
            let card = rgb(.cardContainer, dark: dark)
            let statsCard = rgb(.tealContainer, dark: dark)
            let alpha = Color.statusBadgeTintAlpha(for: dark ? .dark : .light)
            for status in ApplicationStatus.entries {
                let color = rgb(status.color, dark: dark)
                let onCard = contrast(color, card)
                let onBadge = contrast(color, blend(color, over: statsCard, alpha: alpha))
                let mode = dark ? "sombre" : "clair"
                XCTAssertGreaterThanOrEqual(onCard, 4.5, "\(mode) \(status.displayLabel) sur la carte : \(onCard)")
                XCTAssertGreaterThanOrEqual(onBadge, 4.5, "\(mode) \(status.displayLabel) sur le badge : \(onBadge)")
            }
        }
    }

    func test_statusColors_matchTheHexValuesSharedWithAndroid() {
        func hex(_ c: (Double, Double, Double)) -> String {
            String(format: "%02X%02X%02X", Int((c.0 * 255).rounded()), Int((c.1 * 255).rounded()), Int((c.2 * 255).rounded()))
        }
        XCTAssertEqual(hex(rgb(.statusPending, dark: false)), "57535A")
        XCTAssertEqual(hex(rgb(.statusApplied, dark: false)), "0B5E58")
        XCTAssertEqual(hex(rgb(.statusInterview, dark: false)), "913312")
        XCTAssertEqual(hex(rgb(.statusRejected, dark: false)), "9F1616")
        XCTAssertEqual(hex(rgb(.statusAccepted, dark: false)), "235F26")
        XCTAssertEqual(hex(rgb(.statusPending, dark: true)), "CAC4D0")
        XCTAssertEqual(hex(rgb(.statusApplied, dark: true)), "78DDD1")
        XCTAssertEqual(hex(rgb(.statusInterview, dark: true)), "FFB59D")
        XCTAssertEqual(hex(rgb(.statusRejected, dark: true)), "FFB4AB")
        XCTAssertEqual(hex(rgb(.statusAccepted, dark: true)), "9BD99F")
    }

    func test_statusBadgeTintAlpha_isLighterInDarkMode() {
        XCTAssertEqual(Color.statusBadgeTintAlpha(for: .light), 0.16)
        XCTAssertEqual(Color.statusBadgeTintAlpha(for: .dark), 0.08)
    }

    func test_colorHexInit_buildsTheExpectedRgb() {
        let c = rgb(Color(hex: 0xE8734A), dark: false)

        XCTAssertEqual(c.0, 0xE8 / 255.0, accuracy: 0.001)
        XCTAssertEqual(c.1, 0x73 / 255.0, accuracy: 0.001)
        XCTAssertEqual(c.2, 0x4A / 255.0, accuracy: 0.001)
    }

    func test_colorLightDarkInit_resolvesPerInterfaceStyle() {
        let color = Color(light: 0xFFFFFF, dark: 0x000000)

        XCTAssertEqual(rgb(color, dark: false).0, 1.0, accuracy: 0.001)
        XCTAssertEqual(rgb(color, dark: true).0, 0.0, accuracy: 0.001)
    }

    func test_contrastHelper_blackOnWhite_is21ToOne() {
        // Vérifie l'outil de mesure lui-même (valeur de référence WCAG).
        XCTAssertEqual(contrast((0, 0, 0), (1, 1, 1)), 21.0, accuracy: 0.01)
    }
}
