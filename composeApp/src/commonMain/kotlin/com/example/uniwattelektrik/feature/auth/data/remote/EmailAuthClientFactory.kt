package com.example.uniwattelektrik.feature.auth.data.remote

/**
 * Platform-provided email/password client.
 *
 * `actual` implementations:
 *  - androidMain → [FirebaseEmailAuthClient] when `google-services.json` is present,
 *                  otherwise [MockEmailAuthClient] (so the app keeps building).
 *  - iosMain     → [MockEmailAuthClient] until iOS Firebase wiring is added.
 */
expect fun provideEmailAuthClient(): EmailAuthClient

