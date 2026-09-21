import XCTest
import UIKit
@testable import JobTracker

/// Détecte un fichier de police manquant, mal nommé ou non copié dans le bundle : sans lui, SwiftUI retombe
/// silencieusement sur la police système et on ne s'en aperçoit qu'à l'œil.
final class FontRegistrationTests: XCTestCase {

    private let expectedWeights: [AppFontWeight] = [.regular, .medium, .semiBold, .bold]

    func test_registry_registersTheBundledFontFiles() {
        XCTAssertTrue(AppFontRegistry.registered)
    }

    func test_bundle_containsOneTtfFilePerWeight() {
        let names = Bundle.main.urls(forResourcesWithExtension: "ttf", subdirectory: nil)?.map { $0.deletingPathExtension().lastPathComponent } ?? []

        for weight in expectedWeights {
            XCTAssertTrue(names.contains(weight.rawValue), "fichier \(weight.rawValue).ttf absent du bundle (présents : \(names))")
        }
    }

    func test_uiFont_everyWeight_isAvailableAfterRegistration() {
        _ = AppFontRegistry.registered

        for weight in expectedWeights {
            XCTAssertNotNil(UIFont(name: weight.rawValue, size: 16), "police \(weight.rawValue) introuvable après enregistrement")
        }
    }

    func test_uiFont_semiBold_isTheRequestedPostScriptNameAndNotASystemFallback() throws {
        _ = AppFontRegistry.registered

        let font = try XCTUnwrap(UIFont(name: "PlusJakartaSans-SemiBold", size: 16))

        XCTAssertEqual(font.fontName, "PlusJakartaSans-SemiBold")
        XCTAssertEqual(font.familyName, "Plus Jakarta Sans")
    }

    func test_uiFont_requestedSize_isHonoured() throws {
        _ = AppFontRegistry.registered

        XCTAssertEqual(try XCTUnwrap(UIFont(name: AppFontWeight.bold.rawValue, size: 34)).pointSize, 34)
    }

    func test_uiFont_unknownName_isNil() {
        _ = AppFontRegistry.registered

        XCTAssertNil(UIFont(name: "PlusJakartaSans-Inexistante", size: 16))
    }

    func test_appFontWeight_rawValues_matchTheFileNames() {
        XCTAssertEqual(AppFontWeight.regular.rawValue, "PlusJakartaSans-Regular")
        XCTAssertEqual(AppFontWeight.medium.rawValue, "PlusJakartaSans-Medium")
        XCTAssertEqual(AppFontWeight.semiBold.rawValue, "PlusJakartaSans-SemiBold")
        XCTAssertEqual(AppFontWeight.bold.rawValue, "PlusJakartaSans-Bold")
    }

    func test_textStyles_useOnlyWeightsThatAreBundled() {
        let styles: [AppTextStyle] = [.headlineSmall, .titleLarge, .titleMedium, .bodyLarge, .bodyMedium, .labelLarge]

        for style in styles {
            XCTAssertTrue(expectedWeights.contains(style.weight))
        }
    }

    func test_textStyles_matchTheAndroidTypographyScale() {
        XCTAssertEqual([AppTextStyle.headlineSmall.size, AppTextStyle.headlineSmall.lineHeight], [24, 30])
        XCTAssertEqual([AppTextStyle.titleLarge.size, AppTextStyle.titleLarge.lineHeight], [20, 26])
        XCTAssertEqual([AppTextStyle.titleMedium.size, AppTextStyle.titleMedium.lineHeight], [16, 22])
        XCTAssertEqual([AppTextStyle.bodyLarge.size, AppTextStyle.bodyLarge.lineHeight], [16, 22])
        XCTAssertEqual([AppTextStyle.bodyMedium.size, AppTextStyle.bodyMedium.lineHeight], [14, 20])
        XCTAssertEqual([AppTextStyle.labelLarge.size, AppTextStyle.labelLarge.lineHeight], [13, 18])
    }

    func test_textStyleCopy_keepsTheLineHeightLikeKotlinCopy() {
        let big = AppTextStyle.headlineSmall.copy(weight: .bold, size: 34)

        XCTAssertEqual(big.size, 34)
        XCTAssertEqual(big.lineHeight, 30)
    }
}
