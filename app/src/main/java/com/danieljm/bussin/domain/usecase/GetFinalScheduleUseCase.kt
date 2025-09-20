package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.FinalSchedule
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetFinalScheduleUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(stop: String, datum: String, maxAantalDoorkomsten: Int? = null): Result<FinalSchedule> {
        return repository.getFinalScheduleTyped(stop, datum, maxAantalDoorkomsten)
    }
}

