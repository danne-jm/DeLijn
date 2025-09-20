package com.danieljm.bussin.domain.usecase

import com.danieljm.bussin.domain.model.LineDirection
import com.danieljm.bussin.domain.repository.BussinRepository
import javax.inject.Inject

class SearchLinesUseCase @Inject constructor(
    private val repository: BussinRepository
) {
    suspend operator fun invoke(query: String, maxAantalHits: Int? = null): Result<List<LineDirection>> {
        return repository.searchLinesTyped(query, maxAantalHits)
    }
}

