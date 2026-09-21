//
//  LocalDate+Bridge.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import Foundation
import SharedLogic
import Foundation

extension Kotlinx_datetimeLocalDate {
    func toDate() -> Date {
        var comps = DateComponents()
        comps.year = Int(year)
        comps.month = Int(monthNumber)
        comps.day = Int(dayOfMonth)
        return Calendar(identifier: .gregorian).date(from: comps) ?? Date()
    }
}

extension Date {
    func toKotlinLocalDate() -> Kotlinx_datetimeLocalDate {
        let comps = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day], from: self)
        return Kotlinx_datetimeLocalDate(
            year: Int32(comps.year ?? 2026),
            monthNumber: Int32(comps.month ?? 1),
            dayOfMonth: Int32(comps.day ?? 1)
        )
    }
}
