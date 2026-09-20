//
//  JobOfferStatsCard.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

// iosApp/iosApp/Features/JobOffer/JobOfferStatsCard.swift
import SwiftUI
import SharedLogic

struct JobOfferStatsCard: View {
    let offers: [JobOffer]

    private var counts: [(ApplicationStatus, Int)] {
        ApplicationStatus.entries.compactMap { status in
            let count = offers.filter { $0.status == status }.count
            return count > 0 ? (status, count) : nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("\(offers.count)")
                .font(.system(size: 34, weight: .bold))
                .foregroundStyle(Color.tealOnContainer)
            Text(offers.count > 1 ? "candidatures suivies" : "candidature suivie")
                .font(.subheadline)
                .foregroundStyle(Color.tealOnContainer.opacity(0.8))

            if !counts.isEmpty {
                HStack(spacing: 8) {
                    ForEach(counts, id: \.0) { status, count in
                        Text("\(count) \(status.shortLabel)")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(status.color)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(status.color.opacity(0.16))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                .padding(.top, 8)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.tealContainer)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

/*
#Preview {
    JobOfferStatsCard()
}*/
