package com.danieljm.delijn.di

import com.danieljm.delijn.data.remote.api.AuthInterceptor
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {

    fun provideOkHttpClient(apiKey: String = Constants.API_KEY): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(apiKey))
            .addInterceptor(logging)
            .build()
    }

    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    fun provideApiService(retrofit: Retrofit): DeLijnApiService {
        return retrofit.create(DeLijnApiService::class.java)
    }
}

