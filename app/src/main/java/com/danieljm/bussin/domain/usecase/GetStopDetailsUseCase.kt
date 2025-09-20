package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.Stop
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetStopDetailsUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(stop: String): Result<Stop> {
        return repository.getStopDetailsTyped(stop)
    }
}
