import SwiftUI
import SharedLogic

@main
struct iOSApp: App {
    init() {
        KoinInitKt.doInitKoin()
    }
    var body: some Scene {
        WindowGroup {
            //ContentView()
            JobOfferListView(viewModel: KoinHelper().jobOfferListViewModel())
        }
    }
}