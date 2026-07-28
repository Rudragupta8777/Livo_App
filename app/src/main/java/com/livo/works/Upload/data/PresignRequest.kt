package com.livo.works.Upload.data

import androidx.annotation.Keep
@Keep
data class PresignRequest(
    val files: List<PresignFileRequest>
)
