import XCTest
import SharedLogic
@testable import JobLog

/// Pont Swift ↔ Kotlin de l'écran « À propos ». Le contenu et la machine à états sont testés dans sharedLogic (JVM + natif) ;
/// ici on vérifie que Swift les appelle correctement, que la version vient de la vraie config de build, et que les FAITS sur
/// lesquels repose le texte de confidentialité (pas de réseau, pas d'analyse, aucune permission) restent vrais côté iOS.
///
/// NB : `onDeleteAllFinalConfirmed()` n'est volontairement JAMAIS appelé : il viderait la vraie base de l'app hôte sur le simulateur.
final class AboutBridgeTests: XCTestCase {

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

    // MARK: KoinHelper / AboutViewModel

    func test_koinHelper_aboutViewModel_returnsANewInstanceEachTime() {
        let first = KoinHelper().aboutViewModel()
        let second = KoinHelper().aboutViewModel()

        XCTAssertFalse(first === second, "le module Koin déclare le ViewModel en factory")
        first.onCleared(); second.onCleared()
    }

    func test_aboutViewModel_initialState_isIdleWithNothingDeleted() {
        let viewModel = KoinHelper().aboutViewModel()
        defer { viewModel.onCleared() }

        let state = viewModel.state
        XCTAssertEqual(state.deleteStep, .idle)
        XCTAssertFalse(state.isDeleting)
        XCTAssertFalse(state.dataDeleted)
        XCTAssertFalse(state.deletionFailed)
    }

    func test_aboutViewModel_doubleConfirmation_walksThroughBothStepsAndCancelReturnsToIdle() {
        let viewModel = KoinHelper().aboutViewModel()
        defer { viewModel.onCleared() }

        viewModel.onDeleteAllRequested()
        XCTAssertEqual(viewModel.state.deleteStep, .firstConfirmation)
        viewModel.onDeleteAllFirstConfirmed()
        XCTAssertEqual(viewModel.state.deleteStep, .finalConfirmation)
        viewModel.onDeleteAllCancelled()   // on s'arrête AVANT la confirmation finale : rien n'est supprimé
        XCTAssertEqual(viewModel.state.deleteStep, .idle)
        XCTAssertFalse(viewModel.state.dataDeleted)
        XCTAssertFalse(viewModel.state.isDeleting)
    }

    func test_aboutViewModel_firstConfirmationCannotBeSkipped() {
        let viewModel = KoinHelper().aboutViewModel()
        defer { viewModel.onCleared() }

        viewModel.onDeleteAllFirstConfirmed()   // sans demande préalable : ignoré
        XCTAssertEqual(viewModel.state.deleteStep, .idle)
        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllCancelled()
        viewModel.onDeleteAllFirstConfirmed()   // après annulation : ignoré
        XCTAssertEqual(viewModel.state.deleteStep, .idle)
    }

    // MARK: AboutContent vu depuis Swift

    func test_aboutContent_hasTheFourSections_inEachLanguage() {
        let fr = AboutContent.companion.of(language: AppLanguage.fr)
        let en = AboutContent.companion.of(language: AppLanguage.en)

        XCTAssertEqual(fr.sections.map { $0.title },
                       ["Ce que l'app enregistre", "Où vivent tes données", "Ce que l'app ne fait pas", "Tes droits sur tes données"])
        XCTAssertEqual(en.sections.map { $0.title },
                       ["What the app stores", "Where your data lives", "What the app does not do", "Your rights over your data"])
        XCTAssertTrue(fr.sections.allSatisfy { !$0.bullets.isEmpty || $0.intro != nil })
        XCTAssertEqual(fr.deleteAllLabel, "Supprimer toutes mes données")
        XCTAssertEqual(en.deleteAllLabel, "Delete all my data")
        XCTAssertEqual(fr.finalConfirmation.confirmLabel, "Tout supprimer")
        XCTAssertEqual(en.finalConfirmation.confirmLabel, "Delete everything")
    }

    func test_aboutContent_versionLabel_isFormattedBySharedLogic() {
        XCTAssertEqual(AboutContent.companion.of(language: AppLanguage.en).versionLabel(versionName: "2.0", buildNumber: "7"), "Version 2.0 (7)")
    }

    // MARK: version lue dans la vraie configuration de build

    private func xcconfigValue(_ key: String) -> String? {
        source("Configuration/Config.xcconfig").split(separator: "\n")
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .first { $0.hasPrefix("\(key)=") || $0.hasPrefix("\(key) =") }
            .flatMap { $0.split(separator: "=", maxSplits: 1).last }
            .map { $0.trimmingCharacters(in: .whitespaces) }
    }

    func test_appVersion_label_matchesTheBuildConfiguration() throws {
        let marketing = try XCTUnwrap(xcconfigValue("MARKETING_VERSION"), "MARKETING_VERSION introuvable dans Config.xcconfig")
        let build = try XCTUnwrap(xcconfigValue("CURRENT_PROJECT_VERSION"))

        XCTAssertEqual(AppVersion.label(), "Version \(marketing) (\(build))")
    }

    func test_appVersion_label_bundleWithoutVersionKeys_givesTheFallbackNotAnInventedVersion() throws {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent("NoVersion-\(UUID().uuidString).bundle")
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: dir) }
        let plist: [String: Any] = ["CFBundleIdentifier": "test.noversion"]
        try (plist as NSDictionary).write(to: dir.appendingPathComponent("Info.plist"))

        let bundle = try XCTUnwrap(Bundle(url: dir))

        XCTAssertEqual(AppVersion.label(bundle: bundle), AboutContent.companion.of(language: AppLanguage.en).versionLabel(versionName: "", buildNumber: ""))
    }

    // MARK: contact

    func test_contact_mailtoUri_isBuiltBySharedLogicAndIsAValidUrl() throws {
        let uri = AboutContent.companion.contactMailtoUri()

        XCTAssertEqual(uri, "mailto:bizwadan@gmail.com?subject=JobLog%20-%20Contact")
        let url = try XCTUnwrap(URL(string: uri), "URL invalide : openURL ne pourrait pas l'ouvrir")
        XCTAssertEqual(url.scheme, "mailto")
        let components = try XCTUnwrap(URLComponents(url: url, resolvingAgainstBaseURL: false))
        XCTAssertEqual(components.path, AboutContent.companion.CONTACT_EMAIL)
        XCTAssertEqual(components.queryItems?.first(where: { $0.name == "subject" })?.value, AboutContent.companion.CONTACT_SUBJECT)
    }

    func test_contact_labelsAndFallbackMessage_areReadableFromSwift() {
        let fr = AboutContent.companion.of(language: AppLanguage.fr)
        let en = AboutContent.companion.of(language: AppLanguage.en)

        XCTAssertEqual(fr.contactLabel, "Nous contacter")
        XCTAssertEqual(en.contactLabel, "Contact us")
        XCTAssertEqual(AboutContent.companion.CONTACT_EMAIL, "bizwadan@gmail.com")
        XCTAssertTrue(fr.contactNoMailAppMessage.contains(AboutContent.companion.CONTACT_EMAIL))
        XCTAssertTrue(en.contactNoMailAppMessage.contains(AboutContent.companion.CONTACT_EMAIL))
    }

    // MARK: garde-fous de câblage

    func test_listView_hasAnAboutEntryPointInTheToolbar() {
        let list = source("iosApp/Features/JobOffer/JobOfferListView.swift")

        XCTAssertFalse(list.isEmpty, "source introuvable (chemin #filePath)")
        XCTAssertTrue(list.contains("AboutView()"))
        XCTAssertTrue(list.contains("info.circle"))
        XCTAssertTrue(list.contains("entryPointLabel"))
    }

    func test_aboutView_takesItsTextFromTheSharedContentAndDoesNotHardCodeIt() {
        let about = source("iosApp/Features/About/AboutView.swift")

        XCTAssertTrue(about.contains("content.sections"))
        XCTAssertTrue(about.contains("AboutContent.companion.CONTACT_EMAIL") && about.contains("content.contactLabel"))
        XCTAssertTrue(about.contains("content.firstConfirmation") && about.contains("content.finalConfirmation"))
        XCTAssertFalse(about.contains("Supprimer toutes"))
        XCTAssertTrue(about.contains("AppVersion.label()"))
    }

    func test_aboutView_mentionsNoLicenseNorThirdPartyElement() {
        // Décision explicite du propriétaire : aucune licence de dépendance ou de police à l'écran.
        let lower = source("iosApp/Features/About/AboutView.swift").lowercased()
        for word in ["licence", "license", "jakarta", "apache", "thirdparty", "open source"] {
            XCTAssertFalse(lower.contains(word), "« \(word) » dans l'écran À propos")
        }
    }

    func test_aboutView_contactOpensTheSharedMailtoWithOpenURLAndHandlesFailure() {
        let about = source("iosApp/Features/About/AboutView.swift")

        XCTAssertTrue(about.contains("@Environment(\\.openURL)"))
        XCTAssertTrue(about.contains("AboutContent.companion.contactMailtoUri()"))
        XCTAssertTrue(about.contains("openURL(url) { accepted in noMailApp = !accepted }"))
        XCTAssertTrue(about.contains("content.contactNoMailAppMessage"), "sans app de messagerie, l'adresse doit s'afficher")
    }

    func test_contact_addressAndSubjectAreNotHardCodedInSwiftSources() {
        for (path, text) in swiftSources() {
            XCTAssertFalse(text.contains("bizwadan"), "adresse e-mail codée en dur dans \(path)")
            XCTAssertFalse(text.contains("mailto:"), "URI mailto codée en dur dans \(path)")
        }
    }

    func test_aboutView_deleteAllOnlyGoesThroughTheViewModelDoubleConfirmation() {
        let about = source("iosApp/Features/About/AboutView.swift")

        XCTAssertTrue(about.contains("onDeleteAllRequested()"))
        XCTAssertTrue(about.contains("onDeleteAllFirstConfirmed()"))
        XCTAssertTrue(about.contains("onDeleteAllFinalConfirmed()"))
        XCTAssertEqual(about.components(separatedBy: "onDeleteAllFinalConfirmed()").count - 1, 1,
                       "la suppression finale ne doit être déclenchée qu'à un seul endroit (le bouton de l'alerte finale)")
        XCTAssertTrue(about.contains("role: .destructive"))
        XCTAssertFalse(about.contains("deleteAll()"))
        XCTAssertFalse(about.contains("DeleteAllJobOffersUseCase"))
    }

    // MARK: faits sur lesquels repose le texte de confidentialité

    func test_fact_noSwiftSourceUsesTheNetwork() {
        let sources = swiftSources()
        XCTAssertGreaterThan(sources.count, 10, "les sources Swift n'ont pas été trouvées")
        let forbidden = ["URLSession", "URLRequest", "NSURLConnection", "WKWebView", "SFSafariViewController", "Network.framework",
                         "import Network", "Firebase", "Analytics", "AdSupport", "AppTrackingTransparency", "ASIdentifierManager",
                         "CLLocationManager", "CNContactStore", "AVCaptureDevice"]
        for (path, text) in sources where path != "AboutBridgeTests.swift" {
            for word in forbidden {
                XCTAssertFalse(text.contains(word), "« \(word) » dans \(path) : relire AboutContent avant de continuer")
            }
        }
    }

    func test_fact_onlyTheKnownSwiftPackagesAreDeclared() {
        let project = source("iosApp.xcodeproj/project.pbxproj")

        XCTAssertTrue(project.contains("KMP-NativeCoroutines"))
        XCTAssertEqual(project.components(separatedBy: "repositoryURL = ").count - 1, 1,
                       "un paquet Swift a été ajouté (ou retiré) : relire la note « aucun SDK d'analyse » de AboutContent")
        for word in ["firebase", "amplitude", "sentry", "appsflyer", "mixpanel", "google-ads", "GoogleMobileAds"] {
            XCTAssertFalse(project.lowercased().contains(word.lowercased()), "« \(word) » dans le projet : relire AboutContent")
        }
    }

    func test_fact_noPrivacyUsageDescriptionIsDeclared() {
        let project = source("iosApp.xcodeproj/project.pbxproj")
        let plist = source("iosApp/Info.plist")

        XCTAssertFalse(project.contains("UsageDescription"), "une autorisation sensible a été ajoutée : relire AboutContent")
        XCTAssertFalse(plist.contains("UsageDescription"))
    }
}
