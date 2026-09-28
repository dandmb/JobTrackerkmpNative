//
//  AppVersion.swift
//  iosApp
//

import Foundation
import SharedLogic

enum AppVersion {
    /// Libellé de version lu dans la configuration de build réelle du bundle installé
    /// (`MARKETING_VERSION` → `CFBundleShortVersionString`, `CURRENT_PROJECT_VERSION` → `CFBundleVersion`, dans `Config.xcconfig`) :
    /// rien n'est codé en dur. Le format est celui d'Android (`AboutContent.versionLabel`, sharedLogic).
    static func label(bundle: Bundle = .main) -> String {
        let info = bundle.infoDictionary ?? [:]
        return AboutContent.companion.of(language: AppLanguage.current).versionLabel(
            versionName: info["CFBundleShortVersionString"] as? String ?? "",
            buildNumber: info["CFBundleVersion"] as? String ?? ""
        )
    }
}
