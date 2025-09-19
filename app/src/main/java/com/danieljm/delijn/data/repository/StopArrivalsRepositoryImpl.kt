package com.danieljm.delijn.data.repository

import android.util.Log
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.data.remote.dto.RealTimeArrivalsResponseDto
import com.danieljm.delijn.data.remote.dto.ScheduledArrivalsResponseDto
import com.danieljm.delijn.data.mapper.ArrivalInfoMapper
import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine
import com.danieljm.delijn.domain.repository.StopArrivalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StopArrivalsRepositoryImpl(private val api: DeLijnApiService) : StopArrivalsRepository {
    // Simple short-lived in-memory cache to avoid repeated scheduled-arrivals requests
    // for the same stop/date in a short period of time (default TTL 30s).
    private data class CachedScheduled(val arrivals: List<ArrivalInfo>, val fetchedAtMs: Long)
    private val scheduledCache = mutableMapOf<String, CachedScheduled>()
    private val scheduledCacheTtlMs = 30_000L // 30 seconds

    override suspend fun getRealTimeArrivals(entiteitnummer: String, haltenummer: String, servedLines: List<ServedLine>): List<ArrivalInfo> =
        withContext(Dispatchers.IO) {
            val resp: RealTimeArrivalsResponseDto = api.getRealTimeArrivals(entiteitnummer, haltenummer)
            resp.halteDoorkomsten.flatMap { halte ->
                halte.doorkomsten.mapNotNull { ArrivalInfoMapper.fromRealTimeDto(it, servedLines) }
            }.sortedBy { it.remainingMinutes }
        }

    override suspend fun getScheduledArrivals(entiteitnummer: String, haltenummer: String, servedLines: List<ServedLine>): List<ArrivalInfo> =
        withContext(Dispatchers.IO) {
            val date = java.time.LocalDate.now().toString()
            val key = "$entiteitnummer|$haltenummer|$date"

            val cached = scheduledCache[key]
            val now = System.currentTimeMillis()
            if (cached != null && (now - cached.fetchedAtMs) <= scheduledCacheTtlMs) {
                Log.d("StopArrivalsRepo", "Scheduled arrivals cache hit for $key (age=${now - cached.fetchedAtMs}ms)")
                return@withContext cached.arrivals
            }

            Log.d("StopArrivalsRepo", "Fetching scheduled arrivals from API for $key")
            val resp: ScheduledArrivalsResponseDto = api.getScheduledArrivals(entiteitnummer, haltenummer, date)
            val arrivals = resp.halteDoorkomsten.flatMap { halte ->
                halte.doorkomsten.mapNotNull { ArrivalInfoMapper.fromScheduledDto(it, servedLines) }
            }.sortedBy { it.remainingMinutes }

            scheduledCache[key] = CachedScheduled(arrivals, now)
            arrivals
        }
}
