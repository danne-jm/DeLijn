package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine
import com.danieljm.delijn.domain.repository.StopArrivalsRepository

/** Use case to fetch scheduled arrivals (dienstregelingen) for a stop. */
class GetScheduledArrivalsUseCase(private val repository: StopArrivalsRepository) {
    suspend operator fun invoke(entiteitnummer: String, haltenummer: String, servedLines: List<ServedLine>): List<ArrivalInfo> {
        return repository.getScheduledArrivals(entiteitnummer, haltenummer, servedLines)
    }
}

