//
//  JobOffer+Display.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import Foundation
import SharedLogic
import SwiftUI

extension ApplicationStatus {
    var color: Color {
        switch self {
        case .pending: return .statusPending
        case .applied: return .statusApplied
        case .interview: return .statusInterview
        case .rejected: return .statusRejected
        case .accepted: return .statusAccepted
        default: return .gray
        }
    }

    var displayLabel: String {
        switch self {
        case .pending: return "En attente"
        case .applied: return "Postulé"
        case .interview: return "Entretien"
        case .rejected: return "Refusé"
        case .accepted: return "Accepté"
        default: return "?"
        }
    }

    var shortLabel: String {
        switch self {
        case .pending: return "attente"
        case .applied: return "postulé"
        case .interview: return "entretien"
        case .rejected: return "refusé"
        case .accepted: return "accepté"
        default: return "?"
        }
    }
}
