
import Foundation
import SwiftUI
import UIKit

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }

    // MARK: Primaire — Teal
    static let tealPrimary = Color(light: 0x0D6E68, dark: 0x6FD4C8)
    static let onTealPrimary = Color(light: 0xFFFFFF, dark: 0x00201B)
    static let tealContainer = Color(light: 0xB0F1E4, dark: 0x00504A)
    static let tealOnContainer = Color(light: 0x00201B, dark: 0xB0F1E4)

    // MARK: Secondaire — Corail
    static let coralSecondary = Color(light: 0xE8734A, dark: 0xFFB59D)
    static let onCoralSecondary = Color(light: 0xFFFFFF, dark: 0x5B1900)
    static let coralContainer = Color(light: 0xFFDBCB, dark: 0x7D2C0C)
    static let coralOnContainer = Color(light: 0x3A0A00, dark: 0xFFDBCB)

    // MARK: Surfaces
    static let surfaceBase = Color(light: 0xF7FBF9, dark: 0x0F1514)
    static let onSurfaceBase = Color(light: 0x161D1C, dark: 0xDDE4E1)
    static let errorBase = Color(light: 0xBA1A1A, dark: 0xFFB4AB)

    static let appBackground = Color(light: 0xFEF7FF, dark: 0x141218)
    static let onAppBackground = Color(light: 0x1D1B20, dark: 0xE6E0E9)
    static let cardContainer = Color(light: 0xE6E0E9, dark: 0x36343B)
    static let onSurfaceVariant = Color(light: 0x49454F, dark: 0xCAC4D0)
    static let outlineVariant = Color(light: 0xCAC4D0, dark: 0x49454F)

    // MARK: Statuts — variantes clair/sombre, identiques à Color.kt (StatusPalette)
    static let statusPending = Color(light: 0x57535A, dark: 0xCAC4D0)
    static let statusApplied = Color(light: 0x0B5E58, dark: 0x78DDD1)
    static let statusInterview = Color(light: 0x913312, dark: 0xFFB59D)
    static let statusRejected = Color(light: 0x9F1616, dark: 0xFFB4AB)
    static let statusAccepted = Color(light: 0x235F26, dark: 0x9BD99F)

    static func statusBadgeTintAlpha(for scheme: ColorScheme) -> Double {
        scheme == .dark ? 0.08 : 0.16
    }
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}
