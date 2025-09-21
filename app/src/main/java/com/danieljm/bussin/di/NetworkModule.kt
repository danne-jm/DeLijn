package com.danieljm.bussin.di

import com.danieljm.bussin.data.remote.api.BussinApiService
import com.danieljm.bussin.data.remote.auth.AuthInterceptor
import com.danieljm.bussin.data.remote.auth.AuthTokenProvider
import com.danieljm.bussin.data.remote.auth.InMemoryAuthTokenProvider
import com.danieljm.bussin.util.Constants
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val BASE_URL = Constants.BASE_URL

/**
 * Hilt module providing networking dependencies. Declared as an abstract class with a companion
 * object containing `@JvmStatic` `@Provides` methods so the annotation processor sees static
 * provision methods and can generate the necessary code reliably.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    companion object {
        @Provides
        @Singleton
        @JvmStatic
        fun provideMoshi(): Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        @Provides
        @Singleton
        @JvmStatic
        fun provideLoggingInterceptor(): HttpLoggingInterceptor {
            val logger = HttpLoggingInterceptor()
            // Default to BODY for now; adjust as needed
            logger.level = HttpLoggingInterceptor.Level.BODY
            return logger
        }

        @Provides
        @Singleton
        @JvmStatic
        fun provideAuthTokenProvider(): AuthTokenProvider = InMemoryAuthTokenProvider()

        @Provides
        @Singleton
        @JvmStatic
        fun provideAuthInterceptor(tokenProvider: AuthTokenProvider): AuthInterceptor = AuthInterceptor(tokenProvider)

        @Provides
        @Singleton
        @JvmStatic
        fun provideOkHttpClient(
            authInterceptor: AuthInterceptor,
            loggingInterceptor: HttpLoggingInterceptor
        ): OkHttpClient {
            return OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        @JvmStatic
        fun provideRetrofit(moshi: Moshi, okHttpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        @Provides
        @Singleton
        @JvmStatic
        fun provideBussinApiService(retrofit: Retrofit): BussinApiService = retrofit.create(BussinApiService::class.java)
    }
}
