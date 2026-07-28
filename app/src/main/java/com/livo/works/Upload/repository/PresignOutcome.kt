package com.livo.works.Upload.repository

import com.livo.works.Upload.data.PresignedFileDto

/** Result of asking the backend for presigned URLs. */
sealed class PresignOutcome {
    data class Success(val files: List<PresignedFileDto>) : PresignOutcome()
    data class Failure(val message: String) : PresignOutcome()
    object SessionExpired : PresignOutcome()
}
