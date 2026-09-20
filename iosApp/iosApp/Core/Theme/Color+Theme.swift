//
//  Color+Theme.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import Foundation
import SwiftUI

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }

    static let tealPrimary = Color(hex: 0x0D6E68)
    static let tealContainer = Color(hex: 0xB0F1E4)
    static let tealOnContainer = Color(hex: 0x00201B)
    static let coralSecondary = Color(hex: 0xE8734A)

    static let statusPending = Color(hex: 0x79747E)
    static let statusApplied = tealPrimary
    static let statusInterview = coralSecondary
    static let statusRejected = Color(hex: 0xBA1A1A)
    static let statusAccepted = Color(hex: 0x2E7D32)
}
