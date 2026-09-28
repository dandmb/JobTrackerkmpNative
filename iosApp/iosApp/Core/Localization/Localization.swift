
import Foundation
import SharedLogic

func L(_ key: String, _ args: CVarArg...) -> String {
    let format = NSLocalizedString(key, comment: "")
    return args.isEmpty ? format : String(format: format, arguments: args)
}

extension AppLanguage {
    static var current: AppLanguage {
        from(tag: Bundle.main.preferredLocalizations.first)
    }

    static func from(tag: String?) -> AppLanguage {
        AppLanguage.companion.fromTag(tag: tag)
    }
}
