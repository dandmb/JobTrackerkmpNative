//
//  JobOfferCard.swift
//  iosApp
//
//  Created by DAN BIZWA on 19/09/2026.
//

import SwiftUI
import SharedLogic

// Miroir de androidApp/.../ui/joboffer/JobOfferCard.kt.
// Les valeurs (paddings, espacements, tailles) sont celles du code Compose, en pt = dp.
struct JobOfferCard: View {
    let offer: JobOffer
    let onStatusChanged: (ApplicationStatus) -> Void
    let onEditTap: () -> Void

    // Android : LocalDate.Format { dayOfMonth(); ' '; monthName(ENGLISH_ABBREVIATED) } → "20 Sep"
    private static let dateFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateFormat = "d MMM"
        df.locale = Locale(identifier: "en_US_POSIX")
        return df
    }()

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header

            if let location = offer.location {
                locationRow(location)
                    .padding(.top, 4)
            }

            statusChip
                .padding(.top, 8)

            Rectangle()                                   // HorizontalDivider() : 1dp, outlineVariant
                .fill(Color.outlineVariant)
                .frame(height: 1)
                .padding(.top, 12)
                .padding(.bottom, 8)

            dateRow(label: "Postulé", date: offer.appliedDate)
            if let interview = offer.interviewDate {
                dateRow(label: "Entretien", date: interview)
            }
            if let result = offer.resultDate {
                dateRow(label: "Résultat", date: result)
            }

            if let salary = offer.salaryRange {
                Text("💰 \(salary)")
                    .appTextStyle(.bodyMedium)
                    .padding(.top, 4)
            }
        }
        .foregroundStyle(Color.onSurfaceBase)             // contentColorFor(surfaceContainerHighest)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.cardContainer)                  // Card filled : surfaceContainerHighest
        .clipShape(RoundedRectangle(cornerRadius: 12))    // Card : shapes.medium
    }

    // MARK: Titre + entreprise + crayon

    private var header: some View {
        HStack(alignment: .top, spacing: 0) {
            VStack(alignment: .leading, spacing: 0) {
                Text(offer.title.toTitleCase())
                    .appTextStyle(.titleMedium.copy(weight: .semiBold, letterSpacing: 0.15))
                Text(offer.company.capitalizedFirst())
                    .appTextStyle(.bodyMedium)
                    .foregroundStyle(Color.onSurfaceVariant)
            }
            Spacer(minLength: 0)
            // IconButton Material : zone tactile 48dp, icône 24dp centrée
            Button(action: onEditTap) {
                Image(systemName: "pencil")
                    .font(.system(size: 20))
                    .frame(width: 48, height: 48)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Modifier")
        }
    }

    // MARK: Localisation

    private func locationRow(_ location: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: "mappin.and.ellipse")
                .resizable()
                .scaledToFit()
                .frame(width: 14, height: 14)
            Text(location)
                .appTextStyle(.labelLarge)
        }
        .foregroundStyle(Color.onSurfaceVariant)
    }

    // MARK: Statut (cliquable) — AssistChip

    private var statusChip: some View {
        Menu {
            ForEach(ApplicationStatus.entries, id: \.self) { status in
                Button(status.displayLabel) { onStatusChanged(status) }
            }
        } label: {
            // AssistChip : hauteur 32dp, coin small (8dp), contour outlineVariant 1dp,
            // fond transparent, label coloré par le statut, icône ArrowDropDown 18dp (primary).
            HStack(spacing: 8) {
                Text(offer.status.displayLabel)
                    .appTextStyle(.labelLarge)
                    .foregroundStyle(offer.status.color)
                Image(systemName: "arrowtriangle.down.fill")
                    .font(.system(size: 8))
                    .foregroundStyle(Color.tealPrimary)
                    .frame(width: 18, height: 18)
            }
            .padding(.leading, 16)
            .padding(.trailing, 8)
            .frame(height: 32)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(Color.outlineVariant, lineWidth: 1)
            )
            .contentShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
    }

    // MARK: Timeline des dates

    @ViewBuilder
    private func dateRow(label: String, date: Kotlinx_datetimeLocalDate) -> some View {
        HStack {
            Text(label.uppercased())
                .appTextStyle(.labelLarge.copy(weight: .medium, size: 11, letterSpacing: 0.5))
                .foregroundStyle(Color.onSurfaceVariant)
            Spacer()
            Text(Self.dateFormatter.string(from: date.toDate()))
                .appTextStyle(.labelLarge.copy(weight: .semiBold))
        }
    }
}

/*
#Preview {
    JobOfferCard()
}*/
