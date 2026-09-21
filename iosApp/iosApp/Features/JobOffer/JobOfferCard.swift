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

    // Les icônes suivent Dynamic Type comme le texte (sinon elles deviennent minuscules à grande taille de police)
    @ScaledMetric(relativeTo: .body) private var pencilSize: CGFloat = 20
    @ScaledMetric(relativeTo: .body) private var pinSize: CGFloat = 14
    @ScaledMetric(relativeTo: .body) private var chipArrowSize: CGFloat = 8
    @ScaledMetric(relativeTo: .body) private var chipArrowBox: CGFloat = 18

    // « 5 sept. » — Android reproduit les mêmes abréviations (MonthNames français explicites, jour sans zéro).
    static let dateFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateFormat = "d MMM"
        df.locale = Locale(identifier: "fr_FR")
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
                .padding(.top, 2)                         // 8 - 6 : la zone tactile de 44 pt déborde de 6 pt de chaque côté du chip de 32

            Rectangle()                                   // HorizontalDivider() : 1dp, outlineVariant
                .fill(Color.outlineVariant)
                .frame(height: 1)
                .padding(.top, 6)                         // 12 - 6 (idem)
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
                    .font(.system(size: pencilSize))
                    .frame(minWidth: 48, minHeight: 48)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Modifier la candidature \(offer.title)")
        }
    }

    // MARK: Localisation

    private func locationRow(_ location: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: "mappin.and.ellipse")
                .resizable()
                .scaledToFit()
                .frame(width: pinSize, height: pinSize)
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
                    .font(.system(size: chipArrowSize))
                    .foregroundStyle(Color.tealPrimary)
                    .frame(width: chipArrowBox, height: chipArrowBox)
            }
            .padding(.leading, 16)
            .padding(.trailing, 8)
            .frame(minHeight: 32)                          // minHeight : le chip grandit avec Dynamic Type au lieu de tronquer
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(Color.outlineVariant, lineWidth: 1)
            )
            // HIG : zone tactile >= 44 x 44 pt. Le chip reste visuellement à 32 pt.
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Statut : \(offer.status.displayLabel)")
        .accessibilityHint("Ouvre le menu pour changer le statut")
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
        .accessibilityElement(children: .combine)         // VoiceOver : « Postulé, 5 sept. » d'un bloc
    }
}

/*
#Preview {
    JobOfferCard()
}*/
