//
//  String+Case.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import Foundation

// Miroir de androidApp/.../ui/util/TextCase.kt
extension StringProtocol {
    /// Vrai si le mot porte une majuscule après sa première lettre : sigle (SQL, QA, UX) ou nom de marque
    /// en casse mixte (iOS, iPhone, eBay). Ces mots sont saisis volontairement ainsi : on ne les retouche pas.
    fileprivate var hasInnerUppercase: Bool { dropFirst().contains(where: \.isUppercase) }
}

extension String {
    /// Title case : première lettre de chaque mot en majuscule, le reste est laissé tel quel.
    /// Comme Kotlin `split(" ")`, les segments vides sont conservés (les espaces multiples survivent).
    func toTitleCase() -> String {
        split(separator: " ", omittingEmptySubsequences: false)
            .map { word in
                guard !word.hasInnerUppercase, let first = word.first, first.isLowercase else { return String(word) }
                return first.uppercased() + word.dropFirst()
            }
            .joined(separator: " ")
    }

    /// Sentence case : seule la toute première lettre passe en majuscule (sauf si le premier mot est un sigle / une marque en casse mixte).
    func capitalizedFirst() -> String {
        guard let first = first,
              !(split(separator: " ", maxSplits: 1, omittingEmptySubsequences: false).first?.hasInnerUppercase ?? false)
        else { return self }
        return first.uppercased() + dropFirst()
    }
}
