package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.repository.StopRepository
import com.danieljm.delijn.domain.model.LineDirectionStopsResponse

class GetLineDirectionStopsUseCase(
    private val stopRepository: StopRepository
) {
    suspend operator fun invoke(entiteitnummer: String, lijnnummer: String, richting: String): LineDirectionStopsResponse {
        return stopRepository.getLineDirectionStops(entiteitnummer, lijnnummer, richting)
    }
}

