package com.livo.works.Upload.repository

/** Result of a single direct-to-storage PUT. */
sealed class StorageUploadResult {
    object Success : StorageUploadResult()

    /** HTTP 403 — the signed URL is no longer valid and must never be retried as-is. */
    object UrlExpired : StorageUploadResult()

    data class Failure(val message: String) : StorageUploadResult()
}
