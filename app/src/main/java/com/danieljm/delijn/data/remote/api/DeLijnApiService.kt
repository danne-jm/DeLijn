package com.danieljm.delijn.data.remote.api

import com.danieljm.delijn.data.remote.dto.BusDto
import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.data.remote.dto.RouteDto
import com.danieljm.delijn.data.remote.dto.StopDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit interface for De Lijn Open Data endpoints. */
interface DeLijnApiService {
    @GET("stops")
    suspend fun searchStops(@Query("q") query: String): List<StopDto>

    @GET("stops/{id}")
    suspend fun getStop(@Path("id") id: String): StopDto

    @GET("routes/{id}")
    suspend fun getRoute(@Path("id") id: String): RouteDto

    @GET("buses/{id}")
    suspend fun getBus(@Path("id") id: String): BusDto

    @GET("realtime/{stopId}")
    suspend fun getRealTimeForStop(@Path("stopId") stopId: String): List<RealTimeDto>
}
