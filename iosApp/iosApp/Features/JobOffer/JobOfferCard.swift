
import SwiftUI
import SharedLogic

struct JobOfferCard: View {
    let offer: JobOffer
    let onStatusChanged: (ApplicationStatus) -> Void
    let onEditTap: () -> Void

    @ScaledMetric(relativeTo: .body) private var pencilSize: CGFloat = 20
    @ScaledMetric(relativeTo: .body) private var pinSize: CGFloat = 14
    @ScaledMetric(relativeTo: .body) private var chipArrowSize: CGFloat = 8
    @ScaledMetric(relativeTo: .body) private var chipArrowBox: CGFloat = 18

    static func shortDate(_ date: Kotlinx_datetimeLocalDate, language: AppLanguage = .current) -> String {
        ShortDate.shared.format(date: date, language: language)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header

            if let location = offer.location {
                locationRow(location)
                    .padding(.top, 4)
            }

            statusChip
                .padding(.top, 2)

            Rectangle()
                .fill(Color.outlineVariant)
                .frame(height: 1)
                .padding(.top, 6)
                .padding(.bottom, 8)

            dateRow(label: L("date_row_applied"), date: offer.appliedDate)
            if let interview = offer.interviewDate {
                dateRow(label: L("date_row_interview"), date: interview)
            }
            if let result = offer.resultDate {
                dateRow(label: L("date_row_result"), date: result)
            }

            if let salary = offer.salaryRange {
                Text("💰 \(salary)")
                    .appTextStyle(.bodyMedium)
                    .padding(.top, 4)
            }
        }
        .foregroundStyle(Color.onSurfaceBase)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.cardContainer)
        .clipShape(RoundedRectangle(cornerRadius: 12))
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
            Button(action: onEditTap) {
                Image(systemName: "pencil")
                    .font(.system(size: pencilSize))
                    .frame(minWidth: 48, minHeight: 48)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(L("card_edit_a11y", offer.title))
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
            .frame(minHeight: 32)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .strokeBorder(Color.outlineVariant, lineWidth: 1)
            )
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L("card_status_a11y", offer.status.displayLabel))
        .accessibilityHint(L("card_status_hint"))
    }

    // MARK: Timeline des dates

    @ViewBuilder
    private func dateRow(label: String, date: Kotlinx_datetimeLocalDate) -> some View {
        HStack {
            Text(label.uppercased())
                .appTextStyle(.labelLarge.copy(weight: .medium, size: 11, letterSpacing: 0.5))
                .foregroundStyle(Color.onSurfaceVariant)
            Spacer()
            Text(Self.shortDate(date))
                .appTextStyle(.labelLarge.copy(weight: .semiBold))
        }
        .accessibilityElement(children: .combine)
    }
}

/*
#Preview {
    JobOfferCard()
}*/
