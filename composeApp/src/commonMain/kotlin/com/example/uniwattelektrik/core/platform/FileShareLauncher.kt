package com.example.uniwattelektrik.core.platform

import androidx.compose.runtime.Composable

/**
 * Returns a callable that shares a text payload as a file via the platform's
 * share sheet. The host OS handles the chooser (email, Drive, Files, etc).
 *
 * - Android: writes `content` to `cacheDir/shared/<filename>`, gets a
 *   `content://` URI via the bundled FileProvider, and fires `ACTION_SEND`
 *   with `EXTRA_STREAM` so users can email/Drive the CSV directly.
 * - iOS: no-op until `UIActivityViewController` actual is wired.
 *
 * Errors (write failure, no share target installed) are swallowed silently.
 */
@Composable
expect fun rememberFileShareLauncher(): FileShareLauncher

/**
 * Functional handle returned from [rememberFileShareLauncher]. Modelled as an
 * `operator fun invoke` rather than a `typealias` so platform actuals can
 * carry context state (Android needs `LocalContext`) while keeping the
 * call-site identical: `share(content, "report.csv", "text/csv")`.
 */
class FileShareLauncher internal constructor(
    private val impl: (String, String, String) -> Unit,
) {
    operator fun invoke(content: String, filename: String, mimeType: String) =
        impl(content, filename, mimeType)
}
