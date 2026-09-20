//
//  String+Case.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import Foundation

// Miroir de androidApp/.../ui/util/TextCase.kt
extension String {
    /// Title case : première lettre de chaque mot en majuscule, le reste est laissé tel quel.
    /// Comme Kotlin `split(" ")`, les segments vides sont conservés (les espaces multiples survivent).
    func toTitleCase() -> String {
        split(separator: " ", omittingEmptySubsequences: false)
            .map { word in
                guard let first = word.first, first.isLowercase else { return String(word) }
                return first.uppercased() + word.dropFirst()
            }
            .joined(separator: " ")
    }

    /// Sentence case : seule la toute première lettre passe en majuscule.
    func capitalizedFirst() -> String {
        guard let first = first else { return self }
        return first.uppercased() + dropFirst()
    }
}
