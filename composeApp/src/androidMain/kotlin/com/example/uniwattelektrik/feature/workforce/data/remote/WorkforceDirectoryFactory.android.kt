package com.example.uniwattelektrik.feature.workforce.data.remote

actual object WorkforceDirectoryFactory {
    actual fun create(): WorkforceDirectory = FirestoreWorkforceDirectory()
}

