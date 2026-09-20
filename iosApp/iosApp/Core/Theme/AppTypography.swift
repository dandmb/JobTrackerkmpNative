//
//  AppTypography.swift
//  iosApp
//
//  Équivalent de androidApp/.../ui/theme/Type.kt (PlusJakartaSans + AppTypography).
//

import CoreText
import SwiftUI
import UIKit

// MARK: - Police

/// Plus Jakarta Sans, embarquée dans le bundle (Resources/Fonts/*.ttf, licence OFL).
/// Mêmes 4 graisses que côté Android : Normal / Medium / SemiBold / Bold.
enum AppFontWeight: String {
    case regular = "PlusJakartaSans-Regular"
    case medium = "PlusJakartaSans-Medium"
    case semiBold = "PlusJakartaSans-SemiBold"
    case bold = "PlusJakartaSans-Bold"
}

private enum AppFontRegistry {
    /// Enregistre les .ttf du bundle auprès de CoreText, une seule fois.
    /// Fait au runtime pour ne pas dépendre de UIAppFonts (Info.plist) ni de l'emplacement
    /// exact des fichiers dans le bundle.
    static let registered: Bool = {
        guard let root = Bundle.main.resourceURL,
              let files = FileManager.default.enumerator(at: root, includingPropertiesForKeys: nil)
        else { return false }
        for case let url as URL in files where url.pathExtension.lowercased() == "ttf" {
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
        return true
    }()
}

// MARK: - Styles (équivalent des TextStyle de AppTypography)

struct AppTextStyle {
    let weight: AppFontWeight
    let size: CGFloat
    let lineHeight: CGFloat
    var letterSpacing: CGFloat = 0

    /// Équivalent de `TextStyle.copy(...)` : surcharge ponctuelle sans toucher à la hiérarchie.
    func copy(
        weight: AppFontWeight? = nil,
        size: CGFloat? = nil,
        letterSpacing: CGFloat? = nil
    ) -> AppTextStyle {
        AppTextStyle(
            weight: weight ?? self.weight,
            size: size ?? self.size,
            lineHeight: lineHeight,          // comme Kotlin : copy(fontSize:) garde le lineHeight
            letterSpacing: letterSpacing ?? self.letterSpacing
        )
    }

    // Valeurs identiques à Type.kt
    static let headlineSmall = AppTextStyle(weight: .bold, size: 24, lineHeight: 30)
    static let titleLarge = AppTextStyle(weight: .semiBold, size: 20, lineHeight: 26)
    static let titleMedium = AppTextStyle(weight: .semiBold, size: 16, lineHeight: 22)
    static let bodyLarge = AppTextStyle(weight: .regular, size: 16, lineHeight: 22)
    static let bodyMedium = AppTextStyle(weight: .regular, size: 14, lineHeight: 20)
    static let labelLarge = AppTextStyle(weight: .medium, size: 13, lineHeight: 18)
}

// MARK: - Application du style

private struct AppTextStyleModifier: ViewModifier {
    let style: AppTextStyle
    let fixedLineHeight: Bool

    // Suivent Dynamic Type, comme les `sp` Android suivent l'échelle de police.
    @ScaledMetric private var size: CGFloat
    @ScaledMetric private var lineHeight: CGFloat

    init(style: AppTextStyle, fixedLineHeight: Bool) {
        self.style = style
        self.fixedLineHeight = fixedLineHeight
        _size = ScaledMetric(wrappedValue: style.size, relativeTo: .body)
        _lineHeight = ScaledMetric(wrappedValue: style.lineHeight, relativeTo: .body)
    }

    func body(content: Content) -> some View {
        let _ = AppFontRegistry.registered
        let natural = UIFont(name: style.weight.rawValue, size: size)?.lineHeight ?? size * 1.3
        let base = content
            .font(.custom(style.weight.rawValue, fixedSize: size))
            .tracking(style.letterSpacing)
            // Texte sur plusieurs lignes : ramène l'interligne à la valeur Android.
            .lineSpacing(max(0, lineHeight - natural))
        if fixedLineHeight {
            base.frame(height: lineHeight)
        } else {
            // Une ligne : la hauteur de ligne Android est un plancher (22/20/18 dp…).
            base.frame(minHeight: lineHeight)
        }
    }
}

extension View {
    /// Applique un style typographique Android (Plus Jakarta Sans + taille/graisse/interligne).
    /// - Parameter fixedLineHeight: à utiliser quand Android impose un lineHeight plus petit que
    ///   la police (ex. le gros total de la carte stats : 34sp dans un lineHeight de 30sp).
    func appTextStyle(_ style: AppTextStyle, fixedLineHeight: Bool = false) -> some View {
        modifier(AppTextStyleModifier(style: style, fixedLineHeight: fixedLineHeight))
    }
}
