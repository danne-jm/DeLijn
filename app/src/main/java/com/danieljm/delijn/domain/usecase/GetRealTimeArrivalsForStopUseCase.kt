package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.domain.repository.StopRepository

/** Legacy use-case: fetch real-time arrival DTOs by stopId (used by simple search detail). */
class GetRealTimeArrivalsForStopUseCase(private val repository: StopRepository) {
    suspend operator fun invoke(stopId: String): List<RealTimeDto> = repository.getRealTimeArrivals(stopId)
}

