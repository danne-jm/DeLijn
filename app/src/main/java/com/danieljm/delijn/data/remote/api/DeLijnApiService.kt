package com.danieljm.delijn.data.remote.api

import com.danieljm.delijn.data.remote.dto.BusDto
import com.danieljm.delijn.data.remote.dto.LineDirectionDetailDto
import com.danieljm.delijn.data.remote.dto.LineDirectionStopsResponseDto
import com.danieljm.delijn.data.remote.dto.LineDirectionsResponseDto
import com.danieljm.delijn.data.remote.dto.LineDirectionsSearchResponseDto
import com.danieljm.delijn.data.remote.dto.NearbyStopsResponseDto
import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.data.remote.dto.RouteDto
import com.danieljm.delijn.data.remote.dto.StopDto
import com.danieljm.delijn.data.remote.dto.RealTimeArrivalsResponseDto
import com.danieljm.delijn.data.remote.dto.ScheduledArrivalsResponseDto
import com.danieljm.delijn.data.remote.dto.OsrmRouteRequestDto
import com.danieljm.delijn.data.remote.dto.OsrmRouteResponseDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

/** Retrofit interface for De Lijn Open Data endpoints routed through the local proxy */
interface DeLijnApiService {
    @GET("delijn/DLKernOpenData/api/v1/stops")
    suspend fun searchStops(@Query("q") query: String): List<StopDto>

    @GET("delijn/DLKernOpenData/api/v1/stops/{id}")
    suspend fun getStop(@Path("id") id: String): StopDto

    @GET("delijn/DLKernOpenData/api/v1/routes/{id}")
    suspend fun getRoute(@Path("id") id: String): RouteDto

    @GET("delijn/DLKernOpenData/api/v1/buses/{id}")
    suspend fun getBus(@Path("id") id: String): BusDto

    @GET("delijn/DLKernOpenData/api/v1/haltes/{entiteitnummer}/{haltenummer}/real-time")
    suspend fun getRealTimeArrivals(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("haltenummer") haltenummer: String,
        @Query("maxAantalDoorkomsten") maxAantalDoorkomsten: Int = 10
    ): RealTimeArrivalsResponseDto

    // Nearby stops via proxy: /api/delijn/DLKernOpenData/api/v1/haltes/indebuurt/{lat},{lon}
    @GET("delijn/DLKernOpenData/api/v1/haltes/indebuurt/{latitude},{longitude}")
    suspend fun getNearbyStops(
        @Path("latitude") latitude: Double,
        @Path("longitude") longitude: Double,
        @Query("radius") radius: Int = 2500,
        @Query("maxAantalHaltes") maxStops: Int = 50
    ): NearbyStopsResponseDto

    @GET("delijn/DLKernOpenData/api/v1/haltes/{entiteitnummer}/{haltenummer}/lijnrichtingen")
    suspend fun getLineDirectionsForStop(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("haltenummer") haltenummer: String
    ): LineDirectionsResponseDto

    @GET("delijn/DLKernOpenData/api/v1/haltes/{entiteitnummer}/{haltenummer}/dienstregelingen")
    suspend fun getScheduledArrivals(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("haltenummer") haltenummer: String,
        @Query("datum") datum: String
    ): ScheduledArrivalsResponseDto

    // Search API is on DLZoekOpenData via proxy
    @GET("delijn/DLZoekOpenData/v1/zoek/lijnrichtingen/{zoekArgument}")
    suspend fun searchLineDirections(
        @Path("zoekArgument") zoekArgument: String,
        @Query("maxAantalHits") maxAantalHits: Int = 10
    ): LineDirectionsSearchResponseDto

    @GET("delijn/DLKernOpenData/api/v1/lijnen/{entiteitnummer}/{lijnnummer}/lijnrichtingen/{richting}")
    suspend fun getLineDirectionDetail(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("lijnnummer") lijnnummer: String,
        @Path("richting") richting: String
    ): LineDirectionDetailDto

    @GET("delijn/DLKernOpenData/api/v1/lijnen/{entiteitnummer}/{lijnnummer}/lijnrichtingen/{richting}/haltes")
    suspend fun getLineDirectionStops(
        @Path("entiteitnummer") entiteitnummer: String,
        @Path("lijnnummer") lijnnummer: String,
        @Path("richting") richting: String
    ): LineDirectionStopsResponseDto

    // OSRM route geometry proxy endpoint (POST) - backend should implement this
    @POST("route/osrm")
    suspend fun getRouteGeometry(@Body request: OsrmRouteRequestDto): OsrmRouteResponseDto

    // GTFS realtime via proxy. Return raw body for custom parsing (JSON structure varies).
    @GET("delijn/gtfs/v3/realtime")
    suspend fun getGtfsRealtime(
        @Query("json") json: Boolean = true,
        @Query("position") position: Boolean = true,
        @Query("vehicleid") vehicleId: String
    ): ResponseBody
}
