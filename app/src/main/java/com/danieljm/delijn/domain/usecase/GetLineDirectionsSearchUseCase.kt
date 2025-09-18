package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.LineDirectionsSearchResponse
import com.danieljm.delijn.domain.repository.StopRepository

class GetLineDirectionsSearchUseCase(
    private val stopRepository: StopRepository
) {
    suspend operator fun invoke(zoekArgument: String, maxAantalHits: Int = 10): LineDirectionsSearchResponse {
        return stopRepository.searchLineDirections(zoekArgument, maxAantalHits)
    }
}

