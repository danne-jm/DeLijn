package com.danieljm.delijn.data.remote.api

import com.danieljm.delijn.data.remote.dto.BusDto
import com.danieljm.delijn.data.remote.dto.LineDirectionsResponseDto
import com.danieljm.delijn.data.remote.dto.NearbyStopsResponseDto
import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.data.remote.dto.RouteDto
import com.danieljm.delijn.data.remote.dto.StopDto
import com.danieljm.delijn.data.remote.dto.RealTimeArrivalsResponseDto
import com.danieljm.delijn.data.remote.dto.ScheduledArrivalsResponseDto
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

    @GET("haltes/indebuurt/{latitude},{longitude}")
    suspend fun getNearbyStops(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Query("radius") radius: Int = 2500,
        @Query("maxAantalHaltes") maxStops: Int = 50
    ): NearbyStopsResponseDto

    @GET("haltes/{entiteitnummer}/{haltenummer}/lijnrichtingen")
    suspend fun getLineDirectionsForStop(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("haltenummer") haltenummer: String
    ): LineDirectionsResponseDto

    @GET("haltes/{entiteitnummer}/{haltenummer}/real-time")
    suspend fun getRealTimeArrivals(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("haltenummer") haltenummer: String,
        @Query("maxAantalDoorkomsten") maxAantalDoorkomsten: Int = 10
    ): RealTimeArrivalsResponseDto

    @GET("haltes/{entiteitnummer}/{haltenummer}/dienstregelingen")
    suspend fun getScheduledArrivals(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("haltenummer") haltenummer: String,
        @Query("datum") datum: String
    ): ScheduledArrivalsResponseDto
}
