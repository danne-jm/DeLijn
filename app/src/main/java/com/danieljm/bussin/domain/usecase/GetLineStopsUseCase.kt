package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.LineStop
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class GetLineStopsUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(lijnnummer: String, richting: String): Result<List<LineStop>> {
        return repository.getLineStopsTyped(lijnnummer, richting)
    }
}

