package com.livo.works.Upload.data

import androidx.annotation.Keep

/**
 * One presign result. Mirrors the backend's PresignResponse record. The
 * backend returns these positionally - same order, same count as the request's
 * files list - there is no id to correlate by, so callers must zip by index.
 */
@Keep
data class PresignedFileDto(
    val tempPath: String,
    val presignedUrl: String
)
