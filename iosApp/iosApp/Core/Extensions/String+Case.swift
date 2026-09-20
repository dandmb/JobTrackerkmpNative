//
//  String+Case.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import Foundation
extension String {
    func toTitleCase() -> String {
        split(separator: " ")
            .map { $0.prefix(1).uppercased() + $0.dropFirst() }
            .joined(separator: " ")
    }
    func capitalizedFirst() -> String {
        guard let first = first else { return self }
        return first.uppercased() + dropFirst()
    }
}
