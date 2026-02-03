package com.livo.works.Api

import com.livo.works.Search.data.BestHotelResponse
import retrofit2.Response
import retrofit2.http.GET

interface SearchApiService {

    @GET("hotels/best")
    suspend fun getBestHotels(): Response<BestHotelResponse>
}