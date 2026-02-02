package com.livo.works.di

import com.livo.works.Api.AuthApiService
import com.livo.works.Api.BookingApiService
import com.livo.works.Api.HotelApiService
import com.livo.works.Api.PaymentApiService
import com.livo.works.BuildConfig
import com.livo.works.security.AuthAuthenticator
import com.livo.works.security.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = BuildConfig.BACKEND_URL

    @Provides
    @Singleton
    @Named("AuthClient")
    fun provideAuthOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                tokenManager.getAccessToken()?.let {
                    request.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(request.build())
            }
            .build()
    }

    @Provides
    @Singleton
    @Named("ApiClient")
    fun provideApiOkHttpClient(
        tokenManager: TokenManager,
        authenticator: AuthAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                tokenManager.getAccessToken()?.let {
                    request.addHeader("Authorization", "Bearer $it")
                }
                chain.proceed(request.build())
            }
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthRetrofit")
    fun provideAuthRetrofit(@Named("AuthClient") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("ApiRetrofit")
    fun provideApiRetrofit(@Named("ApiClient") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(@Named("AuthRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideHotelApiService(@Named("ApiRetrofit") retrofit: Retrofit): HotelApiService {
        return retrofit.create(HotelApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBookingApiService(@Named("ApiRetrofit") retrofit: Retrofit): BookingApiService {
        return retrofit.create(BookingApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePaymentApiService(@Named("ApiRetrofit") retrofit: Retrofit): PaymentApiService {
        return retrofit.create(PaymentApiService::class.java)
    }
}