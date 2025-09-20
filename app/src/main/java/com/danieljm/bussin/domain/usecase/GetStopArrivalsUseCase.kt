package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.Arrival
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetStopArrivalsUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(stop: String): Result<List<Arrival>> {
        return repository.getStopArrivalsTyped(stop)
    }
}

