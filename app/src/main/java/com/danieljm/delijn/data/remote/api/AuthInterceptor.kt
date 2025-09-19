package com.danieljm.delijn.data.remote.api

import okhttp3.Interceptor
import okhttp3.Response

/** Interceptor that attaches the appropriate API key header depending on the request path. */
class AuthInterceptor(private val coreApiKey: String, private val realtimeApiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath.lowercase()
        val key = if (path.contains("gtfs")) realtimeApiKey else coreApiKey
        val request = original.newBuilder()
            .header("Ocp-Apim-Subscription-Key", key)
            .build()
        return chain.proceed(request)
    }
}
