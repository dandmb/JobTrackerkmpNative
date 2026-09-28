//
//  Color+Theme.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import Foundation
import SwiftUI
import UIKit

// Miroir de androidApp/.../ui/theme/Color.kt et Theme.kt (LightColors / DarkColors).
// Les valeurs "M3 par défaut" (non surchargées côté Android) sont celles des tokens
// Material3 1.12.0-alpha03 lues dans le jar : ColorLightTokens / ColorDarkTokens.

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    /// Couleur qui suit automatiquement le mode clair/sombre du système
    /// (équivalent de `isSystemInDarkTheme()` + LightColors/DarkColors côté Android).
    init(light: UInt32, dark: UInt32) {
        self.init(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }

    // MARK: Primaire — Teal
    static let tealPrimary = Color(light: 0x0D6E68, dark: 0x6FD4C8)          // primary
    static let onTealPrimary = Color(light: 0xFFFFFF, dark: 0x00201B)        // onPrimary
    static let tealContainer = Color(light: 0xB0F1E4, dark: 0x00504A)        // primaryContainer
    static let tealOnContainer = Color(light: 0x00201B, dark: 0xB0F1E4)      // onPrimaryContainer

    // MARK: Secondaire — Corail
    static let coralSecondary = Color(light: 0xE8734A, dark: 0xFFB59D)       // secondary
    static let onCoralSecondary = Color(light: 0xFFFFFF, dark: 0x5B1900)     // onSecondary
    static let coralContainer = Color(light: 0xFFDBCB, dark: 0x7D2C0C)       // secondaryContainer
    static let coralOnContainer = Color(light: 0x3A0A00, dark: 0xFFDBCB)     // onSecondaryContainer

    // MARK: Surfaces
    static let surfaceBase = Color(light: 0xF7FBF9, dark: 0x0F1514)          // surface
    static let onSurfaceBase = Color(light: 0x161D1C, dark: 0xDDE4E1)        // onSurface (texte des cartes)
    static let errorBase = Color(light: 0xBA1A1A, dark: 0xFFB4AB)            // error

    // Valeurs M3 par défaut, non surchargées dans Theme.kt mais réellement affichées sur Android :
    static let appBackground = Color(light: 0xFEF7FF, dark: 0x141218)        // background (fond du Scaffold)
    static let onAppBackground = Color(light: 0x1D1B20, dark: 0xE6E0E9)      // onBackground (texte hors carte)
    static let cardContainer = Color(light: 0xE6E0E9, dark: 0x36343B)        // surfaceContainerHighest (Card)
    static let onSurfaceVariant = Color(light: 0x49454F, dark: 0xCAC4D0)     // onSurfaceVariant (texte secondaire)
    static let outlineVariant = Color(light: 0xCAC4D0, dark: 0x49454F)       // outlineVariant (Divider, contour du chip)

    // MARK: Statuts — variantes clair/sombre, identiques à Color.kt (StatusPalette)
    // Utilisées comme couleur de TEXTE : chaque variante atteint WCAG AA (>= 4.5:1) sur la carte
    // (surfaceContainerHighest) et sur un badge de la carte stats. Teintes de marque conservées.
    static let statusPending = Color(light: 0x57535A, dark: 0xCAC4D0)
    static let statusApplied = Color(light: 0x0B5E58, dark: 0x78DDD1)
    static let statusInterview = Color(light: 0x913312, dark: 0xFFB59D)   // clair : Coral40 #E8734A ne donnait que 2.3:1
    static let statusRejected = Color(light: 0x9F1616, dark: 0xFFB4AB)
    static let statusAccepted = Color(light: 0x235F26, dark: 0x9BD99F)

    /// Opacité de la teinte de fond des badges de statut (StatusBadgeTintLight/Dark côté Android).
    /// Plus faible en sombre : une teinte plus forte éclaircit le fond et fait passer le texte sous 4.5:1.
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
