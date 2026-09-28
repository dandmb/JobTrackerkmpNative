//
//  Localization.swift
//  iosApp
//

import Foundation
import SharedLogic

/// Texte localisé depuis `Localizable.strings` (`en.lproj` = langue de développement, `fr.lproj`), avec arguments `String(format:)`.
/// Pour un `Text("clé")`, `Button("clé")`, `.navigationTitle("clé")`… SwiftUI localise déjà tout seul : `L` sert quand il faut
/// un `String` (arguments, libellés d'accessibilité calculés, `Text(verbatim:)`).
func L(_ key: String, _ args: CVarArg...) -> String {
    let format = NSLocalizedString(key, comment: "")
    return args.isEmpty ? format : String(format: format, arguments: args)
}

extension AppLanguage {
    /// Langue de l'interface pour le contenu PARTAGÉ (sharedLogic) : la première localisation que le système choisit parmi celles
    /// de l'app (`en`, `fr`). Une langue système non gérée (espagnol…) retombe sur la région de développement (`en`) : anglais.
    static var current: AppLanguage {
        from(tag: Bundle.main.preferredLocalizations.first)
    }

    /// Détection à partir d'une balise de langue (testable sans changer la langue de l'appareil).
    static func from(tag: String?) -> AppLanguage {
        AppLanguage.companion.fromTag(tag: tag)
    }
}
