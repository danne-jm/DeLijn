package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.domain.repository.StopRepository

class SearchStopsUseCase(private val repository: StopRepository) {
    suspend operator fun invoke(query: String): List<Stop> {
        return repository.searchStops(query)
    }
}

