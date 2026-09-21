import XCTest
import SharedLogic
@testable import JobLog

/// Anglais (langue de développement) et français : chaque texte existe dans les deux langues, sans texte oublié dans la mauvaise
/// langue ; la langue partagée (sharedLogic) se déduit de la langue que le système choisit parmi celles de l'app.
final class LocalizationTests: XCTestCase {

    private let en = localizedStrings(for: "en")
    private let fr = localizedStrings(for: "fr")
    private let accents = try! NSRegularExpression(pattern: "[àâçéèêëîïôûùüÿœ«»]", options: [.caseInsensitive])

    /// Textes identiques dans les deux langues par nature.
    private let sameInBothLanguages: Set<String> = ["sort_az", "sort_za", "form_section_dates", "form_field_notes",
                                                    "form_salary_min_short", "form_salary_max_short"]

    private func placeholders(_ value: String) -> [String] {
        let regex = try! NSRegularExpression(pattern: "%(\\d+\\$)?[@d]")
        return regex.matches(in: value, range: NSRange(value.startIndex..., in: value))
            .map { String(value[Range($0.range, in: value)!]) }.map { $0.replacingOccurrences(of: "$", with: "") }.sorted()
    }

    func test_bothLanguageFiles_areLoadedFromTheBundle() {
        XCTAssertGreaterThan(en.count, 40, "Localizable.strings (en) introuvable ou vide")
        XCTAssertGreaterThan(fr.count, 40)
    }

    func test_everyKey_existsInEnglishAndFrench() {
        XCTAssertEqual(Set(en.keys).subtracting(fr.keys), [], "clés sans traduction française")
        XCTAssertEqual(Set(fr.keys).subtracting(en.keys), [], "clés françaises sans version anglaise")
    }

    func test_placeholders_areTheSameInBothLanguages() {
        for (key, value) in en {
            XCTAssertEqual(placeholders(value), placeholders(fr[key] ?? ""), "arguments différents pour « \(key) »")
        }
    }

    func test_englishFile_containsNoFrenchAccentOrGuillemet() {
        for (key, value) in en {
            let range = NSRange(value.startIndex..., in: value)
            XCTAssertNil(accents.firstMatch(in: value, range: range), "texte français dans en.lproj : \(key) = \(value)")
        }
    }

    func test_everyText_isTranslated_exceptTheKnownInvariants() {
        for (key, value) in en where !sameInBothLanguages.contains(key) {
            XCTAssertNotEqual(value, fr[key], "« \(key) » identique en anglais et en français")
        }
    }

    func test_noValue_isBlank() {
        for (key, value) in en.merging(fr.mapKeys { "fr:" + $0 }, uniquingKeysWith: { a, _ in a }) {
            XCTAssertFalse(value.trimmingCharacters(in: .whitespaces).isEmpty, "valeur vide : \(key)")
        }
    }

    func test_L_returnsTheTranslationOfTheAppLanguage_andFormatsItsArguments() {
        let current = Bundle.main.preferredLocalizations.first == "fr" ? "fr" : "en"
        XCTAssertEqual(L("list_title"), localized("list_title", in: current))
        XCTAssertEqual(L("list_no_results", "zzz"), localized("list_no_results", in: current, "zzz"))
        XCTAssertEqual(localized("list_no_results", in: "en", "zzz"), "No results for “zzz”")
        XCTAssertEqual(localized("list_no_results", in: "fr", "zzz"), "Aucun résultat pour « zzz »")
        XCTAssertEqual(localized("onboarding_page_indicator", in: "en", 2, 3), "Page 2 of 3")
        XCTAssertEqual(localized("onboarding_page_indicator", in: "fr", 2, 3), "Page 2 sur 3")
    }

    // MARK: langue partagée

    func test_appLanguage_fromTag_frenchOnlyForFrench_everythingElseIsEnglish() {
        for tag in ["fr", "fr-FR", "fr_CA", "FR"] { XCTAssertEqual(AppLanguage.from(tag: tag), AppLanguage.fr, tag) }
        for tag in ["en", "en-US", "es", "de-DE", "ja", "pt-BR", "", "und"] { XCTAssertEqual(AppLanguage.from(tag: tag), AppLanguage.en, tag) }
        XCTAssertEqual(AppLanguage.from(tag: nil), AppLanguage.en)
    }

    func test_appLanguage_current_followsTheLocalizationTheSystemPicksForTheApp() {
        let picked = Bundle.main.preferredLocalizations.first
        XCTAssertEqual(AppLanguage.current, AppLanguage.from(tag: picked))
        XCTAssertTrue(["en", "fr"].contains(picked ?? ""), "la localisation choisie doit être une des langues de l'app : \(picked ?? "nil")")
    }

    func test_bundle_declaresEnglishAsDevelopmentRegionAndBothLanguages() {
        XCTAssertEqual(Bundle.main.developmentLocalization, "en")
        XCTAssertEqual(Set(Bundle.main.localizations.filter { $0 != "Base" }), ["en", "fr"])
    }

    func test_anUnsupportedSystemLanguage_resolvesToEnglish() {
        // Ce que fait le système : parmi les localisations de l'app, la première préférée ; à défaut, la région de développement.
        let picked = Bundle.preferredLocalizations(from: Bundle.main.localizations, forPreferences: ["es-ES", "de"])
        XCTAssertEqual(picked.first, "en")
        XCTAssertEqual(Bundle.preferredLocalizations(from: Bundle.main.localizations, forPreferences: ["fr-CA", "en"]).first, "fr")
        XCTAssertEqual(Bundle.preferredLocalizations(from: Bundle.main.localizations, forPreferences: ["en-GB", "fr"]).first, "en")
    }

    // MARK: aucun texte français codé en dur dans les sources Swift

    func test_swiftSources_haveNoFrenchLiteral() {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().appendingPathComponent("iosApp")
        let files = FileManager.default.enumerator(at: root, includingPropertiesForKeys: nil)?.compactMap { $0 as? URL }.filter { $0.pathExtension == "swift" } ?? []
        XCTAssertGreaterThan(files.count, 10)
        let literal = try! NSRegularExpression(pattern: "\"[^\"\\n]*[àâçéèêëîïôûùüÿœÀÂÇÉÈÊËÎÏÔÛÙÜŸŒ«»][^\"\\n]*\"")
        var offenders: [String] = []
        for file in files {
            let text = (try? String(contentsOf: file, encoding: .utf8)) ?? ""
            for (index, line) in text.components(separatedBy: "\n").enumerated() {
                let code = line.trimmingCharacters(in: .whitespaces)
                if code.hasPrefix("//") || code.hasPrefix("*") || code.hasPrefix("/*") { continue }
                let head = code.components(separatedBy: "//").first ?? code
                if literal.firstMatch(in: head, range: NSRange(head.startIndex..., in: head)) != nil {
                    offenders.append("\(file.lastPathComponent):\(index + 1): \(code.prefix(80))")
                }
            }
        }
        XCTAssertEqual(offenders, [], "texte français codé en dur (utiliser Localizable.strings ou le contenu partagé)")
    }
}

private extension Dictionary where Key == String {
    func mapKeys(_ transform: (String) -> String) -> [String: Value] {
        Dictionary(uniqueKeysWithValues: map { (transform($0.key), $0.value) })
    }
}
