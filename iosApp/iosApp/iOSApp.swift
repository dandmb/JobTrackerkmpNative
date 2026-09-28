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
            AppRootView()   // splash + onboarding + liste ; crée le ViewModel de la liste une seule fois
        }
    }
}