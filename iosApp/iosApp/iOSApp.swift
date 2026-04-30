import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        // Required by the Firebase iOS SDK before any FirebaseAuth /
        // Firestore call. Reads GoogleService-Info.plist from the bundle.
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

