package com.danieljm.delijn.di

import com.danieljm.delijn.data.local.dao.BusDao
import com.danieljm.delijn.data.local.dao.RouteDao
import com.danieljm.delijn.data.local.dao.StopDao
import com.danieljm.delijn.data.remote.api.DeLijnApiService
import com.danieljm.delijn.domain.usecase.GetBusDetailsUseCase
import com.danieljm.delijn.domain.usecase.GetRealTimeArrivalsUseCase
import com.danieljm.delijn.domain.usecase.GetRouteDetailsUseCase
import com.danieljm.delijn.domain.usecase.GetStopDetailsUseCase
import com.danieljm.delijn.domain.usecase.SearchStopsUseCase

object UseCaseModule {
    fun provideSearchStopsUseCase(api: DeLijnApiService, stopDao: StopDao) =
        SearchStopsUseCase(RepositoryModule.provideStopRepository(api, stopDao))

    fun provideGetStopDetailsUseCase(api: DeLijnApiService, stopDao: StopDao) =
        GetStopDetailsUseCase(RepositoryModule.provideStopRepository(api, stopDao))

    fun provideGetBusDetailsUseCase(api: DeLijnApiService, busDao: BusDao) =
        GetBusDetailsUseCase(RepositoryModule.provideBusRepository(api, busDao))

    fun provideGetRouteDetailsUseCase(api: DeLijnApiService, routeDao: RouteDao) =
        GetRouteDetailsUseCase(RepositoryModule.provideRouteRepository(api, routeDao))

    fun provideGetRealTimeArrivalsUseCase(api: DeLijnApiService, stopDao: StopDao) =
        GetRealTimeArrivalsUseCase(RepositoryModule.provideStopRepository(api, stopDao))
}
