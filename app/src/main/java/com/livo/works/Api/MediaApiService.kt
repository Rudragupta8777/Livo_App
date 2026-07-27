package com.livo.works.Api

import com.livo.works.Auth.data.ApiResponse
import com.livo.works.Upload.data.PresignRequest
import com.livo.works.Upload.data.PresignedFileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MediaApiService {
    // Response is a flat array positionally matching the request's files list -
    // there is no per-file id, so order must be preserved on both sides.
    @POST("admin/media/presign")
    suspend fun presign(
        @Body request: PresignRequest
    ): Response<ApiResponse<List<PresignedFileDto>>>
}