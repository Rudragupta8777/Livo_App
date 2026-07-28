package com.livo.works.Upload.data

import androidx.annotation.Keep

/** One file entry inside the presign request body. Mirrors the backend's PresignFileRequest record. */
@Keep
data class PresignFileRequest(
    val fileName: String,
    val contentType: String,
    val contentLength: Long
)
