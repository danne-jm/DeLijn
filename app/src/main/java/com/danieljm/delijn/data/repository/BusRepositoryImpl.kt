package com.danieljm.delijn.data.repository

import com.danieljm.delijn.data.local.dao.BusDao
import com.danieljm.delijn.data.mapper.BusMapper
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.domain.model.Bus
import com.danieljm.delijn.domain.repository.BusRepository

/** Implementation of BusRepository (wires local + remote). */
class BusRepositoryImpl(
    private val api: DeLijnApiService,
    private val dao: BusDao
) : BusRepository {
    override suspend fun getBusDetails(busId: String): Bus? {
        val cached = dao.getBusById(busId)
        if (cached != null) return BusMapper.entityToDomain(cached)
        val dto = api.getBus(busId)
        val entity = BusMapper.dtoToEntity(dto)
        dao.insertBus(entity)
        return BusMapper.entityToDomain(entity)
    }
}
