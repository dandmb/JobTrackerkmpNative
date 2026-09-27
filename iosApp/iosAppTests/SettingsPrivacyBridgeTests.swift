import XCTest
import SharedLogic
@testable import JobLog

/// Pont Swift ↔ Kotlin des écrans « Réglages » et « Politique de confidentialité » (miroir de `AboutBridgeTests`).
/// Le texte VERBATIM de la politique de confidentialité est verrouillé côté Kotlin (`PrivacyContentTest`, sharedLogic,
/// JVM + natif) : ici on vérifie seulement que Swift l'affiche sans le recopier ni le modifier.
final class SettingsPrivacyBridgeTests: XCTestCase {

    private var projectDir: URL {
        URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
    }

    private func source(_ relativePath: String) -> String {
        (try? String(contentsOf: projectDir.appendingPathComponent(relativePath), encoding: .utf8)) ?? ""
    }

    private func swiftSources() -> [(path: String, text: String)] {
        let root = projectDir.appendingPathComponent("iosApp")
        guard let files = FileManager.default.enumerator(at: root, includingPropertiesForKeys: nil) else { return [] }
        return files.compactMap { $0 as? URL }.filter { $0.pathExtension == "swift" }
            .map { ($0.lastPathComponent, (try? String(contentsOf: $0, encoding: .utf8)) ?? "") }
    }

    // MARK: SettingsContent / PrivacyContent vus depuis Swift

    func test_settingsContent_labelsAreReadableFromSwift() {
        let fr = SettingsContent.companion.of(language: AppLanguage.fr)
        let en = SettingsContent.companion.of(language: AppLanguage.en)

        XCTAssertEqual(fr.screenTitle, "Réglages")
        XCTAssertEqual(en.screenTitle, "Settings")
        XCTAssertEqual(fr.aboutRowLabel, "À propos")
        XCTAssertEqual(fr.privacyRowLabel, "Politique de confidentialité")
    }

    func test_privacyContent_hasEightSections_inEachLanguage() {
        let fr = PrivacyContent.companion.of(language: AppLanguage.fr)
        let en = PrivacyContent.companion.of(language: AppLanguage.en)

        XCTAssertEqual(fr.sections.count, 8)
        XCTAssertEqual(en.sections.count, 8)
        XCTAssertEqual(PrivacyContent.companion.ONLINE_URL, "https://gist.github.com/dandmb/c7461e74a35140d79e657a19eb66566f")
    }

    // MARK: garde-fous de câblage — SettingsView

    func test_settingsView_listsAboutAndPrivacy_fromTheSharedContent() {
        let settings = source("iosApp/Features/Settings/SettingsView.swift")

        XCTAssertFalse(settings.isEmpty, "source introuvable (chemin #filePath)")
        XCTAssertTrue(settings.contains("SettingsContent"))
        XCTAssertTrue(settings.contains("content.aboutRowLabel"))
        XCTAssertTrue(settings.contains("content.privacyRowLabel"))
        XCTAssertTrue(settings.contains("AboutView()"))
        XCTAssertTrue(settings.contains("PrivacyView()"))
    }

    // MARK: garde-fous de câblage — PrivacyView

    func test_privacyView_takesItsTextFromTheSharedContent_andDoesNotHardCodeIt() {
        let privacy = source("iosApp/Features/Privacy/PrivacyView.swift")

        XCTAssertFalse(privacy.isEmpty, "source introuvable (chemin #filePath)")
        XCTAssertTrue(privacy.contains("PrivacyContent"))
        XCTAssertTrue(privacy.contains("content.sections"))
        XCTAssertTrue(privacy.contains("content.documentTitle"))
        XCTAssertTrue(privacy.contains("content.introText"))
        // Aucun morceau du texte français verbatim ne doit être recopié dans la vue (seul PrivacyContent le porte).
        XCTAssertFalse(privacy.contains("Résumé"))
        XCTAssertFalse(privacy.contains("JobTracker"))
    }

    func test_privacyView_linksToTheOnlineVersion_keptIdenticalByRequirement() {
        let privacy = source("iosApp/Features/Privacy/PrivacyView.swift")

        XCTAssertTrue(privacy.contains("PrivacyContent.companion.ONLINE_URL"))
        XCTAssertTrue(privacy.contains("content.onlineVersionLabel"))
        XCTAssertTrue(privacy.contains("content.onlineVersionHint"), "accessibilityHint approprié attendu")
        XCTAssertFalse(privacy.contains("gist.github.com"), "l'URL doit venir de PrivacyContent.ONLINE_URL, pas être codée en dur dans la vue")
    }

    // MARK: pas de duplication du texte verbatim ailleurs dans les sources Swift

    func test_noSwiftSourceHardCodesThePrivacyText() {
        for (path, text) in swiftSources() where path != "SettingsPrivacyBridgeTests.swift" {
            XCTAssertFalse(text.contains("JobTracker"), "texte de confidentialité recopié en dur dans \(path)")
        }
    }
}
