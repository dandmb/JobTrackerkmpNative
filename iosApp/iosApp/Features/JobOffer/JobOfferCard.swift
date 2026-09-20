//
//  JobOfferCard.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import SwiftUI
import SharedLogic

struct JobOfferCard: View {
    let offer: JobOffer
    let onStatusChanged: (ApplicationStatus) -> Void
    let onEditTap: () -> Void

    private static let dateFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateFormat = "d MMM"
        df.locale = Locale(identifier: "fr_FR")
        return df
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(offer.title.toTitleCase())
                        .font(.system(size: 17, weight: .semibold))
                    Text(offer.company.capitalizedFirst())
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button(action: onEditTap) {
                    Image(systemName: "pencil")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
            }

            if let location = offer.location {
                Label(location, systemImage: "mappin.and.ellipse")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Menu {
                ForEach(ApplicationStatus.entries, id: \.self) { status in
                    Button(status.displayLabel) { onStatusChanged(status) }
                }
            } label: {
                HStack(spacing: 4) {
                    Text(offer.status.displayLabel)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 10, weight: .bold))
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(offer.status.color)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(offer.status.color.opacity(0.16))
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)

            Divider()

            dateRow(label: "Postulé", date: offer.appliedDate)
            if let interview = offer.interviewDate {
                dateRow(label: "Entretien", date: interview)
            }
            if let result = offer.resultDate {
                dateRow(label: "Résultat", date: result)
            }

            if let salary = offer.salaryRange {
                Text("💰 \(salary)")
                    .font(.subheadline)
            }
        }
        .padding(16)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    @ViewBuilder
    private func dateRow(label: String, date: Kotlinx_datetimeLocalDate) -> some View {
        HStack {
            Text(label.uppercased())
                .font(.system(size: 11, weight: .medium))
                .tracking(0.5)
                .foregroundStyle(.secondary)
            Spacer()
            Text(Self.dateFormatter.string(from: date.toDate()))
                .font(.system(size: 13, weight: .semibold))
        }
    }
}

/*
#Preview {
    JobOfferCard()
}*/
