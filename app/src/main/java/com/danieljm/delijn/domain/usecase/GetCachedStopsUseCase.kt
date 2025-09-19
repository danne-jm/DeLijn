package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.domain.repository.StopRepository

class GetCachedStopsUseCase(
    private val repository: StopRepository
) {
    suspend operator fun invoke(): List<Stop> {
        return repository.getCachedStops()
    }
}
