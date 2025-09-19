package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine
import com.danieljm.delijn.domain.repository.StopArrivalsRepository

/**
 * Use case: fetch real-time arrivals for a stopId and return domain ArrivalInfo objects.
 * Internally this should be composed by first resolving stop details and line directions
 * (those use-cases are provided separately via DI). For legacy compatibility the class
 * accepts the required dependencies and returns domain models.
 */
class GetRealTimeArrivalsForStopUseCase(
    private val getStopDetailsUseCase: GetStopDetailsUseCase,
    private val getLineDirectionsForStopUseCase: GetLineDirectionsForStopUseCase,
    private val stopArrivalsRepository: StopArrivalsRepository
) {
    suspend operator fun invoke(stopId: String): List<ArrivalInfo> {
        val stop = getStopDetailsUseCase(stopId) ?: return emptyList()
        val directionsResponse = getLineDirectionsForStopUseCase(stop.entiteitnummer, stop.halteNummer)

        val servedLines = directionsResponse.lijnrichtingen.map { line ->
            ServedLine(
                lineId = line.lijnnummer,
                lineName = line.lijnnummer,
                omschrijving = line.omschrijving,
                asFromTo = line.omschrijving,
                entiteitnummer = line.entiteitnummer,
                richting = line.richting
            )
        }

        return stopArrivalsRepository.getRealTimeArrivals(stop.entiteitnummer, stop.halteNummer, servedLines)
    }
}
