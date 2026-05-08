package com.example.uniwattelektrik.core.storage

/**
 * Cloudinary free-tier credentials.
 *
 * Steps to fill in:
 *  1. Sign up at https://cloudinary.com  (no credit card required)
 *  2. From the Cloudinary Console → Dashboard, copy your **Cloud name**
 *  3. Settings → Upload → Add upload preset → Mode = **Unsigned** → Save
 *     Copy the preset name.
 *  4. Replace the two placeholders below.
 */
internal object CloudinaryConfig {
    /** e.g. "dxyz1234a" — shown on the Cloudinary Console Dashboard */
    const val CLOUD_NAME    = "dcxjmlivz"

    /** Unsigned preset created in Cloudinary Settings → Upload */
    const val UPLOAD_PRESET = "ml_defaults"
}
