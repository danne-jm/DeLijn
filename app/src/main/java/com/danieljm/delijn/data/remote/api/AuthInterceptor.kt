package com.danieljm.delijn.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

/** Simple interceptor that can add API key headers to requests. */
class AuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Ocp-Apim-Subscription-Key", apiKey)
            .build()
        return chain.proceed(request)
    }
}

