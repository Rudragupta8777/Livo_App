package com.livo.works.di

import com.livo.works.Api.AuthApiService
import com.livo.works.Api.BookingApiService
import com.livo.works.Api.ManagerApiService
import com.livo.works.Api.PaymentApiService
import com.livo.works.Api.RoleApiService
import com.livo.works.Api.RoomApiService
import com.livo.works.Api.SearchApiService
import com.livo.works.BuildConfig
import com.livo.works.Search.repository.SearchRepository
import com.livo.works.security.AuthAuthenticator
import com.livo.works.security.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = BuildConfig.BACKEND_URL

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

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
            .addInterceptor(logging)
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
            .addInterceptor(logging)
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
    fun provideBookingApiService(@Named("ApiRetrofit") retrofit: Retrofit): BookingApiService {
        return retrofit.create(BookingApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePaymentApiService(@Named("ApiRetrofit") retrofit: Retrofit): PaymentApiService {
        return retrofit.create(PaymentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchApi(@Named("ApiRetrofit") retrofit: Retrofit): SearchApiService {
        return retrofit.create(SearchApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRoleApiService(@Named("ApiRetrofit") retrofit: Retrofit): RoleApiService {
        return retrofit.create(RoleApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideManagerApiService(@Named("ApiRetrofit") retrofit: Retrofit): ManagerApiService {
        return retrofit.create(ManagerApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRoomApiService(@Named("ApiRetrofit") retrofit: Retrofit): RoomApiService {
        return retrofit.create(RoomApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchRepository(api: SearchApiService): SearchRepository {
        return SearchRepository(api)
    }
}