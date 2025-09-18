package com.danieljm.delijn.data.repository

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
            val resp: ScheduledArrivalsResponseDto = api.getScheduledArrivals(entiteitnummer, haltenummer, date)
            resp.halteDoorkomsten.flatMap { halte ->
                halte.doorkomsten.mapNotNull { ArrivalInfoMapper.fromScheduledDto(it, servedLines) }
            }.sortedBy { it.remainingMinutes }
        }
}
