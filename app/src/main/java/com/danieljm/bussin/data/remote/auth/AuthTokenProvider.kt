package com.danieljm.bussin.data.remote.auth

/**
 * Small abstraction to provide an auth token for network requests.
 * Implementation can read from SharedPreferences, DataStore, or any secure store.
 */
interface AuthTokenProvider {
    /**
     * Return the current auth token, or null if none exists.
     */
    suspend fun getToken(): String?
}

/**
 * A simple in-memory implementation (useful for testing). Replace with a
 * DataStore-backed implementation in production.
 */
class InMemoryAuthTokenProvider(private var token: String? = null) : AuthTokenProvider {
    override suspend fun getToken(): String? = token

    fun setToken(newToken: String?) {
        token = newToken
    }
}

