package com.danieljm.bussin.data.repository

import android.util.Log
import com.danieljm.bussin.data.local.dao.LineStopDao
import com.danieljm.bussin.data.local.dao.StopDao
import com.danieljm.bussin.data.local.entity.LineStopEntity
import com.danieljm.bussin.data.local.entity.StopEntity
import com.danieljm.bussin.data.mapper.*
import com.danieljm.bussin.data.remote.api.BussinApiService
import com.danieljm.bussin.data.remote.dto.*
import com.danieljm.bussin.domain.model.*
import com.danieljm.bussin.domain.repository.BussinRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Repository implementation that keeps networking details inside the data layer.
 * It now supports typed domain-returning methods for many endpoints.
 */
class BussinRepositoryImpl @Inject constructor(
    private val api: BussinApiService,
    private val moshi: Moshi,
    private val stopDao: StopDao,
    private val lineStopDao: LineStopDao
) : BussinRepository {

    private suspend fun <T> call(callBlock: suspend () -> retrofit2.Response<T>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = callBlock()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val text = when (body) {
                        is ResponseBody -> body.string()
                        is String -> body
                        else -> body?.toString() ?: ""
                    }
                    Result.success(text)
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun discovery(): Result<String> = call { api.discovery() }

    override suspend fun getNearbyStops(stop: String, latitude: Double, longitude: Double, radius: Int?, maxAantalHaltes: Int?): Result<String> =
        // If caller passes an empty stop id (""), calling the path-based endpoint produces
        // "/stops//nearby" (double slash). Use the coords-only endpoint in that case.
        if (stop.trim().isEmpty()) {
            Log.d("BussinRepo", "getNearbyStops: using coords-only endpoint (stop blank)")
            // Always omit radius from the network call; backend will apply default radius if omitted
            call { api.getNearbyStopsByCoords(latitude, longitude, null, maxAantalHaltes) }
        } else {
            Log.d("BussinRepo", "getNearbyStops: using path endpoint for stop=$stop")
            // Always omit radius from the network call; backend will apply default radius if omitted
            call { api.getNearbyStops(stop, latitude, longitude, null, maxAantalHaltes) }
        }

    override suspend fun getStopLines(stop: String): Result<String> = call { api.getStopLines(stop) }

    override suspend fun getStopArrivals(stop: String): Result<String> = call { api.getStopArrivals(stop) }

    override suspend fun getStopDetails(stop: String): Result<String> = call { api.getStopDetails(stop) }

    override suspend fun getDienstregelingen(stop: String, datum: String): Result<String> = call { api.getDienstregelingen(stop, datum) }

    override suspend fun getFinalSchedule(stop: String, datum: String, maxAantalDoorkomsten: Int?): Result<String> =
        call { api.getFinalSchedule(stop, datum, maxAantalDoorkomsten) }

    override suspend fun searchLines(query: String, maxAantalHits: Int?): Result<String> = call { api.searchLines(query, maxAantalHits) }

    override suspend fun getLineStops(lijnnummer: String, richting: String): Result<String> = call { api.getLineStops(lijnnummer, richting) }

    override suspend fun getVehiclePosition(vehicleId: String): Result<String> = call { api.getVehiclePosition(vehicleId) }

    override suspend fun getVehicleRoute(vehicleId: String, halteNumbers: String?, lijnnummer: String?, richting: String?): Result<String> =
        call { api.getVehicleRouteByHaltes(vehicleId, halteNumbers, lijnnummer, richting) }

    // Typed methods
    override suspend fun getNearbyStopsTyped(stop: String, latitude: Double, longitude: Double, radius: Int?, maxAantalHaltes: Int?): Result<List<Stop>> {
        return withContext(Dispatchers.IO) {
            try {
                val useCoords = stop.trim().isEmpty()
                if (useCoords) Log.d("BussinRepo", "getNearbyStopsTyped: using coords-only endpoint (stop blank)") else Log.d("BussinRepo", "getNearbyStopsTyped: using path endpoint for stop=$stop")
                val resp = if (useCoords) {
                    // Always omit radius; maxantalhaltes from the network call; backend will apply default radius if omitted
                    api.getNearbyStopsByCoords(latitude, longitude, null, null)
                } else {
                    // Always omit radius; maxantalhaltes from the network call; backend will apply default radius if omitted
                    api.getNearbyStops(stop, latitude, longitude, null, null)
                }
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val adapter = moshi.adapter(NearbyStopsResponseDto::class.java)
                    val dto = adapter.fromJson(body)
                    val haltes = dto?.haltes ?: emptyList()
                    val stops = haltes.map { StopMapper.fromDto(it) }

                    // Persist to Room (simple overwrite for now)
                    try {
                        val entities = stops.map { s -> StopEntity.fromDomain(s) }
                        stopDao.insertAll(entities)
                    } catch (_: Exception) {
                        // ignore persistence errors, still return data
                    }

                    Result.success(stops)
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getStopDetailsTyped(stop: String): Result<Stop> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.getStopDetails(stop)
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val adapter = moshi.adapter(HalteDto::class.java)
                    val dto = adapter.fromJson(body)
                    val stopDto = dto ?: HalteDto(type = null, id = stop, haltenummer = stop, naam = null)
                    val stopDomain = StopMapper.fromDto(stopDto)

                    try {
                        stopDao.insertAll(listOf(StopEntity.fromDomain(stopDomain)))
                    } catch (_: Exception) {
                        // ignore
                    }

                    Result.success(stopDomain)
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchLinesTyped(query: String, maxAantalHits: Int?): Result<List<LineDirection>> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.searchLines(query, maxAantalHits)
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val singleAdapter = moshi.adapter(LineSearchResponseDto::class.java)
                    val single = try { singleAdapter.fromJson(body) } catch (_: Exception) { null }
                    if (single != null) {
                        return@withContext Result.success(listOf(LineDirectionMapper.fromDto(single)))
                    }

                    // Try parse as list
                    val type = Types.newParameterizedType(List::class.java, LineSearchResponseDto::class.java)
                    val listAdapter = moshi.adapter<List<LineSearchResponseDto>>(type)
                    val listDto = try { listAdapter.fromJson(body) } catch (_: Exception) { null }
                    val results = listDto?.map { LineDirectionMapper.fromDto(it) } ?: emptyList()
                    return@withContext Result.success(results)
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getLineStopsTyped(lijnnummer: String, richting: String): Result<List<LineStop>> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.getLineStops(lijnnummer, richting)
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val adapter = moshi.adapter(LineStopsResponseDto::class.java)
                    val dto = try { adapter.fromJson(body) } catch (_: Exception) { null }
                    val haltes = dto?.haltes ?: emptyList()
                    val stops = haltes.map { LineStopMapper.fromDto(it) }

                    try {
                        val entities = stops.map { LineStopEntity.fromDomain(it) }
                        lineStopDao.insertAll(entities)
                    } catch (_: Exception) { }

                    Result.success(stops)
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getStopArrivalsTyped(stop: String): Result<List<Arrival>> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.getStopArrivals(stop)
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val adapter = moshi.adapter(ArrivalsResponseDto::class.java)
                    val dto = try { adapter.fromJson(body) } catch (_: Exception) { null }
                    val halteList = dto?.halteDoorkomsten ?: emptyList()
                    val arrivals = halteList.flatMap { hd -> hd.doorkomsten?.map { ArrivalsMapper.fromDoorkomst(it) } ?: emptyList() }
                    Result.success(arrivals)
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getFinalScheduleTyped(stop: String, datum: String, maxAantalDoorkomsten: Int?): Result<FinalSchedule> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.getFinalSchedule(stop, datum, maxAantalDoorkomsten)
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val adapter = moshi.adapter(FinalScheduleResponseDto::class.java)
                    val dto = try { adapter.fromJson(body) } catch (_: Exception) { null }
                    val final = dto?.let { FinalScheduleMapper.fromDto(it) }
                    if (final != null) {
                        Result.success(final)
                    } else {
                        Result.failure(Exception("Failed to parse final schedule"))
                    }
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getVehiclePositionTyped(vehicleId: String): Result<VehiclePosition> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.getVehiclePosition(vehicleId)
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val adapter = moshi.adapter(VehiclePositionDto::class.java)
                    val dto = try { adapter.fromJson(body) } catch (_: Exception) { null }
                    val inner = dto?.vehicleWrapper?.vehicle
                    val pos = VehicleMapper.fromVehicleInner(inner)
                    if (pos != null) Result.success(pos) else Result.failure(Exception("Failed to parse vehicle position"))
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getVehicleRouteTyped(vehicleId: String, halteNumbers: String?, lijnnummer: String?, richting: String?): Result<VehicleRoute> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.getVehicleRouteByHaltes(vehicleId, halteNumbers, lijnnummer, richting)
                if (resp.isSuccessful) {
                    val body = resp.body()?.string() ?: ""
                    val adapter = moshi.adapter(VehicleRouteResponseDto::class.java)
                    val dto = try { adapter.fromJson(body) } catch (_: Exception) { null }
                    val route = dto?.let { VehicleMapper.fromRouteDto(it) }
                    if (route != null) Result.success(route) else Result.failure(Exception("Failed to parse vehicle route"))
                } else {
                    Result.failure(HttpException(resp))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
