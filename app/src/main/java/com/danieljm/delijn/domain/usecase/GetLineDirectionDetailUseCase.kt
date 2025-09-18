package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.LineDirectionSearch
import com.danieljm.delijn.domain.repository.StopRepository

class GetLineDirectionDetailUseCase(
    private val stopRepository: StopRepository
) {
    suspend operator fun invoke(entiteitnummer: String, lijnnummer: String, richting: String): LineDirectionSearch? {
        return stopRepository.getLineDirectionDetail(entiteitnummer, lijnnummer, richting)
    }
}

