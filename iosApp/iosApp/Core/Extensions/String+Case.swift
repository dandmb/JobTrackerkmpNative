
import Foundation

extension StringProtocol {
    // Sigles (SQL, QA, UX) et marques en casse mixte (iOS, eBay) : casse volontaire, jamais retouchée.
    fileprivate var hasInnerUppercase: Bool { dropFirst().contains(where: \.isUppercase) }
}

extension String {
    func toTitleCase() -> String {
        split(separator: " ", omittingEmptySubsequences: false)
            .map { word in
                guard !word.hasInnerUppercase, let first = word.first, first.isLowercase else { return String(word) }
                return first.uppercased() + word.dropFirst()
            }
            .joined(separator: " ")
    }

    func capitalizedFirst() -> String {
        guard let first = first,
              !(split(separator: " ", maxSplits: 1, omittingEmptySubsequences: false).first?.hasInnerUppercase ?? false)
        else { return self }
        return first.uppercased() + dropFirst()
    }
}
