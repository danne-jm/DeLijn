package com.danieljm.delijn.di

import com.danieljm.delijn.data.local.dao.BusDao
import com.danieljm.delijn.data.local.dao.RouteDao
import com.danieljm.delijn.data.local.dao.StopDao
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.data.repository.BusRepositoryImpl
import com.danieljm.delijn.data.repository.RouteRepositoryImpl
import com.danieljm.delijn.data.repository.StopRepositoryImpl
import com.danieljm.delijn.domain.repository.BusRepository
import com.danieljm.delijn.domain.repository.RouteRepository
import com.danieljm.delijn.domain.repository.StopRepository

/** Simple provider to wire repository interfaces to their implementations. Replace with DI framework bindings. */
object RepositoryModule {
    // Accept the required dependencies and forward them to the concrete implementations.
    fun provideStopRepository(api: DeLijnApiService, dao: StopDao): StopRepository = StopRepositoryImpl(api, dao)
    fun provideBusRepository(api: DeLijnApiService, dao: BusDao): BusRepository = BusRepositoryImpl(api, dao)
    fun provideRouteRepository(api: DeLijnApiService, dao: RouteDao): RouteRepository = RouteRepositoryImpl(api, dao)
}
