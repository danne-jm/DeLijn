package com.danieljm.delijn.di

import com.danieljm.delijn.data.remote.api.AuthInterceptor
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object NetworkModule {

    fun provideOkHttpClient(apiKey: String = Constants.API_KEY): OkHttpClient {
        // Enable verbose logging to inspect request/response bodies during development
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKey))
            .addInterceptor(logging)
            .build()
    }

    fun provideRetrofit(client: OkHttpClient): Retrofit {
        // Create Moshi instance with KotlinJsonAdapterFactory to support Kotlin data classes
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun provideApiService(retrofit: Retrofit): DeLijnApiService {
        return retrofit.create(DeLijnApiService::class.java)
    }
}
