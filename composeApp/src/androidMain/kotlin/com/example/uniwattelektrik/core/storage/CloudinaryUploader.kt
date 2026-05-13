package com.example.uniwattelektrik.core.storage

import android.content.Context
import android.net.Uri
import com.example.uniwattelektrik.core.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Thin Cloudinary uploader — uses only OkHttp + android.net.Uri.
 * No Cloudinary SDK dependency needed.
 *
 * Supports both content:// (gallery / camera) and file:// URIs.
 *
 * Usage:
 *   val url = CloudinaryUploader.upload(
 *       context   = appContext,
 *       contentUri= "content://...",
 *       folder    = "employee-photos/adminId",
 *       publicId  = "userId",          // optional — Cloudinary uses it as filename
 *   )
 */
internal object CloudinaryUploader {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Upload [contentUri] to Cloudinary and return the permanent HTTPS URL.
     *
     * @param context      Any Android context (used for ContentResolver).
     * @param contentUri   content:// or file:// URI string to upload.
     * @param folder       Cloudinary sub-folder, e.g. "employee-photos/admin123".
     * @param publicId     Optional file name in Cloudinary (no extension).
     * @param resourceType "image" / "video" / "raw" / "auto". For audio (.m4a)
     *                     pass "video" — Cloudinary stores audio under the
     *                     video resource type. Defaults to "image" so existing
     *                     photo upload call sites keep working unchanged.
     * @param mimeType     Multipart MIME for the file part (e.g. "audio/mp4").
     * @param extension    File extension on the upload form-data filename.
     */
    suspend fun upload(
        context: Context,
        contentUri: String,
        folder: String,
        publicId: String? = null,
        resourceType: String = "image",
        mimeType: String = "image/jpeg",
        extension: String = "jpg",
    ): String = withContext(Dispatchers.IO) {
        AppLog.i("Cloudinary", "upload type=$resourceType folder=$folder uri=$contentUri")

        val bytes = readBytes(context, contentUri)
        AppLog.i("Cloudinary", "  bytes=${bytes.size}")

        val uploadUrl =
            "https://api.cloudinary.com/v1_1/${CloudinaryConfig.CLOUD_NAME}/$resourceType/upload"

        val bodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
            .addFormDataPart("folder", folder)
            .addFormDataPart(
                "file",
                "${publicId ?: "upload"}.$extension",
                bytes.toRequestBody(mimeType.toMediaType()),
            )

        publicId?.let { bodyBuilder.addFormDataPart("public_id", it) }

        val request = Request.Builder()
            .url(uploadUrl)
            .post(bodyBuilder.build())
            .build()

        val response = client.newCall(request).execute()
        val body     = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            AppLog.e("Cloudinary", "upload failed ${response.code}: $body")
            throw RuntimeException("Cloudinary upload failed (${response.code}): $body")
        }

        val secureUrl = JSONObject(body).optString("secure_url", "")
        if (secureUrl.isBlank()) {
            AppLog.e("Cloudinary", "secure_url missing in response: $body")
            throw RuntimeException("Cloudinary: secure_url not found in response")
        }

        AppLog.i("Cloudinary", "  ↳ $secureUrl")
        secureUrl
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun readBytes(context: Context, uriString: String): ByteArray {
        val uri = Uri.parse(uriString)
        return when {
            uri.scheme == "file" -> {
                File(uri.path!!).readBytes()
            }
            else -> {
                // content:// — camera or gallery
                context.contentResolver
                    .openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Cannot open stream for URI: $uriString")
            }
        }
    }
}
