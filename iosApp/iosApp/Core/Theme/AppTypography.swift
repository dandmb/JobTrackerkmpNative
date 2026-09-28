
import CoreText
import SwiftUI
import UIKit

// MARK: - Police

enum AppFontWeight: String {
    case regular = "PlusJakartaSans-Regular"
    case medium = "PlusJakartaSans-Medium"
    case semiBold = "PlusJakartaSans-SemiBold"
    case bold = "PlusJakartaSans-Bold"
}

// Enregistrement au runtime pour ne pas dépendre de UIAppFonts (Info.plist).
enum AppFontRegistry {
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

    func copy(
        weight: AppFontWeight? = nil,
        size: CGFloat? = nil,
        letterSpacing: CGFloat? = nil
    ) -> AppTextStyle {
        AppTextStyle(
            weight: weight ?? self.weight,
            size: size ?? self.size,
            lineHeight: lineHeight,
            letterSpacing: letterSpacing ?? self.letterSpacing
        )
    }

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
            .lineSpacing(max(0, lineHeight - natural))
        if fixedLineHeight {
            base.frame(height: lineHeight)
        } else {
            base.frame(minHeight: lineHeight)
        }
    }
}

extension View {
    func appTextStyle(_ style: AppTextStyle, fixedLineHeight: Bool = false) -> some View {
        modifier(AppTextStyleModifier(style: style, fixedLineHeight: fixedLineHeight))
    }
}
