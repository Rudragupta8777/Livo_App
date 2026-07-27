package com.livo.works.Upload.data

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class MediaUploadItem(
    val clientId: String,
    val uri: Uri,
    val state: MediaUploadState = MediaUploadState.PENDING,
    val progress: Int = 0,
    val tempPath: String? = null,
    val errorMessage: String? = null
)
