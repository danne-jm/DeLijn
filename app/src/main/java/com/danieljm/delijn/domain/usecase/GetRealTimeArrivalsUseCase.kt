package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.data.remote.dto.RealTimeDto
import com.danieljm.delijn.domain.repository.StopRepository

/** Use case to fetch real-time arrivals for a stop. */
class GetRealTimeArrivalsUseCase(private val repository: StopRepository) {
    suspend operator fun invoke(stopId: String): List<RealTimeDto> {
        return repository.getRealTimeArrivals(stopId)
    }
}
