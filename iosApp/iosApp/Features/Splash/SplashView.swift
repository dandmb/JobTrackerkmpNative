
import SwiftUI

struct SplashView: View {
    var body: some View {
        ZStack {
            Color(hex: 0x0D6E68)
                .ignoresSafeArea()
            JobLogMark()
                .frame(width: 100)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("JobLog")
    }
}
