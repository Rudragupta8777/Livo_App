package com.livo.works.Upload.repository

import android.util.Log
import com.livo.works.Api.MediaApiService
import com.livo.works.Upload.data.PresignFileRequest
import com.livo.works.Upload.data.PresignRequest
import com.livo.works.Upload.data.ProgressRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Owns the two network halves of the upload pipeline:
 *  1. presign  -> our backend (authenticated)
 *  2. PUT      -> Supabase Storage (direct, no backend involved, no auth header)
 *
 * Image bytes never pass through Spring Boot.
 */
@Singleton
class MediaUploadRepository @Inject constructor(
    private val api: MediaApiService,
    @Named("UploadClient") private val uploadClient: OkHttpClient
) {

    suspend fun requestPresignedUrls(files: List<PresignFileRequest>): PresignOutcome =
        withContext(Dispatchers.IO) {
            try {
                val response = api.presign(PresignRequest(files))
                val body = response.body()

                when {
                    response.isSuccessful && body?.data != null ->
                        PresignOutcome.Success(body.data)

                    response.code() == 401 -> PresignOutcome.SessionExpired

                    else -> {
                        Log.e(TAG, "presign: HTTP ${response.code()} - ${body?.error?.message}")
                        PresignOutcome.Failure(body?.error?.message ?: "Could not prepare the upload")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "presign: request threw", e)
                PresignOutcome.Failure("Network Error")
            }
        }

    suspend fun uploadToStorage(
        uploadUrl: String,
        file: File,
        contentType: String,
        onProgress: (Int) -> Unit
    ): StorageUploadResult = withContext(Dispatchers.IO) {
        try {
            val body = ProgressRequestBody(file, contentType.toMediaTypeOrNull(), onProgress)

            val request = Request.Builder()
                .url(uploadUrl)
                .put(body)
                .header("Content-Type", contentType)
                .build()

            val call = uploadClient.newCall(request)
            // Abandon the socket as soon as the caller's coroutine is cancelled
            // (e.g. the user removed the image mid-upload).
            coroutineContext[Job]?.invokeOnCompletion { cause ->
                if (cause != null) call.cancel()
            }

            call.execute().use { response ->
                when {
                    response.isSuccessful -> StorageUploadResult.Success
                    response.code == 403 -> StorageUploadResult.UrlExpired
                    else -> StorageUploadResult.Failure("Upload failed (${response.code})")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadToStorage: PUT threw", e)
            StorageUploadResult.Failure(e.localizedMessage ?: "Upload failed")
        }
    }

    private companion object {
        const val TAG = "MediaUploadRepository"
    }
}
