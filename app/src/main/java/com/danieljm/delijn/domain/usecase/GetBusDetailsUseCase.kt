package com.danieljm.delijn.domain.usecase

import com.danieljm.delijn.domain.model.Bus
import com.danieljm.delijn.domain.repository.BusRepository

class GetBusDetailsUseCase(private val repository: BusRepository) {
    suspend operator fun invoke(busId: String): Bus? {
        return repository.getBusDetails(busId)
    }
}

