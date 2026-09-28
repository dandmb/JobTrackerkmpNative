
import Foundation
import SharedLogic

enum AppVersion {
    static func label(bundle: Bundle = .main) -> String {
        let info = bundle.infoDictionary ?? [:]
        return AboutContent.companion.of(language: AppLanguage.current).versionLabel(
            versionName: info["CFBundleShortVersionString"] as? String ?? "",
            buildNumber: info["CFBundleVersion"] as? String ?? ""
        )
    }
}
