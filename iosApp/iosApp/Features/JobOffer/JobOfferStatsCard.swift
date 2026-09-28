
import SwiftUI
import SharedLogic

struct JobOfferStatsCard: View {
    let offers: [JobOffer]
    @Environment(\.colorScheme) private var colorScheme

    private var counts: [(ApplicationStatus, Int)] {
        ApplicationStatus.entries.compactMap { status in
            let count = offers.filter { $0.status == status }.count
            return count > 0 ? (status, count) : nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                Text("\(offers.count)")
                    .appTextStyle(.headlineSmall.copy(weight: .bold, size: 34), fixedLineHeight: true)
                    .foregroundStyle(Color.tealOnContainer)
                Text(L(offers.count > 1 ? "stats_tracked_other" : "stats_tracked_one"))
                    .appTextStyle(.bodyMedium)
                    .foregroundStyle(Color.tealOnContainer.opacity(0.8))
            }
            .accessibilityElement(children: .combine)

            if !counts.isEmpty {
                FlowLayout(spacing: 8) {
                    ForEach(counts, id: \.0) { status, count in
                        Text(status.shortLabel(count: count))
                            .appTextStyle(.labelLarge.copy(weight: .semiBold))
                            .foregroundStyle(status.color)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(status.color.opacity(Color.statusBadgeTintAlpha(for: colorScheme)))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 16)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.tealContainer)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

/*
#Preview {
    JobOfferStatsCard()
}*/
