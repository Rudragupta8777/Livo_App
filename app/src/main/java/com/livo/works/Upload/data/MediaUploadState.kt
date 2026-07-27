package com.livo.works.Upload.data

import androidx.annotation.Keep

@Keep
enum class MediaUploadState {
    PENDING,
    UPLOADING,
    UPLOADED,
    FAILED,
    REMOVED
}