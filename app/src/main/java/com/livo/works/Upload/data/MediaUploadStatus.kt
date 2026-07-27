package com.livo.works.Upload.data

import androidx.annotation.Keep
@Keep
data class MediaUploadStatus(
    val total: Int = 0,
    val uploaded: Int = 0,
    val isUploading: Boolean = false,
    val hasFailed: Boolean = false
) {
    val isReadyToSubmit: Boolean
        get() = total > 0 && uploaded == total && !isUploading && !hasFailed
}