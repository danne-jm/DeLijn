package com.danieljm.bussin.data.remote.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds Authorization header when a token is available.
 * The AuthTokenProvider is suspendable; runBlocking is used because OkHttp's Interceptor
 * API is synchronous. In future this could be improved with a token cache.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val token = try {
            runBlocking { tokenProvider.getToken() }
        } catch (t: Throwable) {
            null
        }

        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")

        token?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
