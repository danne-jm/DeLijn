package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.LineDirectionsResponse
import com.danieljm.delijn.domain.repository.StopRepository

class GetLineDirectionsForStopUseCase(
    private val stopRepository: StopRepository
) {
    suspend operator fun invoke(entiteitnummer: String, haltenummer: String): LineDirectionsResponse {
        return stopRepository.getLineDirectionsForStop(entiteitnummer, haltenummer)
    }
}
