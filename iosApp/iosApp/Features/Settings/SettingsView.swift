
import SwiftUI
import SharedLogic

struct SettingsView: View {
    private let content = SettingsContent.companion.of(language: AppLanguage.current)

    var body: some View {
        List {
            NavigationLink {
                AboutView()
            } label: {
                Label(content.aboutRowLabel, systemImage: "info.circle")
            }
            NavigationLink {
                PrivacyView()
            } label: {
                Label(content.privacyRowLabel, systemImage: "hand.raised")
            }
        }
        .navigationTitle(content.screenTitle)
        .navigationBarTitleDisplayMode(.inline)
    }
}
