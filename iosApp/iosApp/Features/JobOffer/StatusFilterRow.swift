
import SwiftUI
import SharedLogic

struct StatusFilterRow: View {
    @Binding var selectedStatuses: Set<ApplicationStatus>
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                pill(label: L("filter_all"), isSelected: selectedStatuses.isEmpty, tint: .tealPrimary) {
                    selectedStatuses = []
                }
                ForEach(ApplicationStatus.entries, id: \.self) { status in
                    let isSelected = selectedStatuses.contains(status)
                    pill(label: status.displayLabel, isSelected: isSelected, tint: status.color) {
                        if isSelected { selectedStatuses.remove(status) } else { selectedStatuses.insert(status) }
                    }
                }
            }
            .padding(.horizontal, 16)
        }
    }

    @ViewBuilder
    private func pill(label: String, isSelected: Bool, tint: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .appTextStyle(.labelLarge)
                .foregroundStyle(isSelected ? tint : Color.onSurfaceVariant)
                .padding(.horizontal, 14)
                .frame(minHeight: 32)
                .background(isSelected ? tint.opacity(Color.statusBadgeTintAlpha(for: colorScheme)) : Color.clear)
                .overlay(Capsule().strokeBorder(isSelected ? tint : Color.outlineVariant, lineWidth: 1))
                .clipShape(Capsule())
                .frame(minHeight: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
