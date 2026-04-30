package com.example.uniwattelektrik.feature.auth.data.remote

/**
 * Android wires the [AdminDirectory] to Firestore. The Firebase SDK reads
 * `google-services.json` at app start, so no extra parameters are needed.
 */
actual object AdminDirectoryFactory {
    actual fun create(): AdminDirectory = FirestoreAdminDirectory()
}

