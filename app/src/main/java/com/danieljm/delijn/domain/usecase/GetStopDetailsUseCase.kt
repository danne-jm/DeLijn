package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.Stop
import com.danieljm.delijn.domain.repository.StopRepository

class GetStopDetailsUseCase(private val repository: StopRepository) {
    suspend operator fun invoke(stopId: String): Stop? {
        return repository.getStopDetails(stopId)
    }
}

