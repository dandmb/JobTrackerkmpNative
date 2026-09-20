//
//  JobOfferStatsCard.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

// iosApp/iosApp/Features/JobOffer/JobOfferStatsCard.swift
import SwiftUI
import SharedLogic

// Miroir de androidApp/.../ui/joboffer/JobOfferStatsCard.kt.
struct JobOfferStatsCard: View {
    let offers: [JobOffer]

    private var counts: [(ApplicationStatus, Int)] {
        ApplicationStatus.entries.compactMap { status in
            let count = offers.filter { $0.status == status }.count
            return count > 0 ? (status, count) : nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // headlineSmall.copy(fontSize = 34.sp, Bold) — le lineHeight reste celui de headlineSmall (30sp)
            Text("\(offers.count)")
                .appTextStyle(.headlineSmall.copy(weight: .bold, size: 34), fixedLineHeight: true)
                .foregroundStyle(Color.tealOnContainer)
            Text(offers.count > 1 ? "candidatures suivies" : "candidature suivie")
                .appTextStyle(.bodyMedium)
                .foregroundStyle(Color.tealOnContainer.opacity(0.8))

            if !counts.isEmpty {
                HStack(spacing: 8) {
                    ForEach(counts, id: \.0) { status, count in
                        Text("\(count) \(status.shortLabel)")
                            .appTextStyle(.labelLarge.copy(weight: .semiBold))
                            .foregroundStyle(status.color)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(status.color.opacity(0.16))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 16)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.tealContainer)                  // primaryContainer
        .clipShape(RoundedRectangle(cornerRadius: 12))    // Card : shapes.medium (était 16)
    }
}

/*
#Preview {
    JobOfferStatsCard()
}*/
