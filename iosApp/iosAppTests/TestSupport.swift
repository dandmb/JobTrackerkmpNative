import Foundation
import SharedLogic
@testable import JobLog

/// Fabrique un `JobOffer` Kotlin valide ; on ne surcharge que ce que le test veut observer.
func makeOffer(
    id: Int64 = 0,
    title: String = "Développeur Kotlin",
    company: String = "Acme",
    location: String? = nil,
    salaryRange: String? = nil,
    applied: (Int32, Int32, Int32) = (2026, 9, 5),
    interview: (Int32, Int32, Int32)? = nil,
    result: (Int32, Int32, Int32)? = nil,
    status: ApplicationStatus = .applied
) -> JobOffer {
    func date(_ t: (Int32, Int32, Int32)) -> Kotlinx_datetimeLocalDate {
        Kotlinx_datetimeLocalDate(year: t.0, monthNumber: t.1, dayOfMonth: t.2)
    }
    return JobOffer(
        id: id, title: title, company: company, url: nil, location: location, source: nil,
        salaryRange: salaryRange, appliedDate: date(applied),
        interviewDate: interview.map(date), resultDate: result.map(date),
        status: status, notes: nil
    )
}


/// Textes de `Localizable.strings` pour UNE langue (`en` / `fr`), lus dans le bundle de l'app — indépendant de la langue de l'appareil.
func localizedStrings(for language: String) -> [String: String] {
    guard let path = Bundle.main.path(forResource: "Localizable", ofType: "strings", inDirectory: nil, forLocalization: language),
          let dictionary = NSDictionary(contentsOfFile: path) as? [String: String] else { return [:] }
    return dictionary
}

/// Chaîne localisée dans une langue précise (sans dépendre de la langue de l'appareil).
func localized(_ key: String, in language: String, _ args: CVarArg...) -> String {
    let format = localizedStrings(for: language)[key] ?? "⟨manquant: \(key)⟩"
    return args.isEmpty ? format : String(format: format, arguments: args)
}
