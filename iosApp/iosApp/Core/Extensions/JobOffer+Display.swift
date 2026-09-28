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
        case .pending: return L("status_pending")
        case .applied: return L("status_applied")
        case .interview: return L("status_interview")
        case .rejected: return L("status_rejected")
        case .accepted: return L("status_accepted")
        default: return "?"
        }
    }

    /// Libellé court AVEC le nombre (badges de la carte de statistiques : « 2 applied » / « 2 postulé »).
    /// Règle du pluriel : `count > 1` → `_other`, sinon `_one` (identique à Android).
    func shortLabel(count: Int) -> String {
        let suffix = count > 1 ? "_other" : "_one"
        switch self {
        case .pending: return L("stats_pending" + suffix, count)
        case .applied: return L("stats_applied" + suffix, count)
        case .interview: return L("stats_interview" + suffix, count)
        case .rejected: return L("stats_rejected" + suffix, count)
        case .accepted: return L("stats_accepted" + suffix, count)
        default: return "?"
        }
    }
}
