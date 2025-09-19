package com.danieljm.delijn.di

import com.danieljm.delijn.data.local.AppDatabase
import com.danieljm.delijn.data.local.dao.BusDao
import com.danieljm.delijn.data.local.dao.RouteDao
import com.danieljm.delijn.data.local.dao.StopDao
import com.danieljm.delijn.data.repository.BusRepositoryImpl
import com.danieljm.delijn.data.repository.RouteRepositoryImpl
import com.danieljm.delijn.data.repository.StopRepositoryImpl
import com.danieljm.delijn.data.repository.StopArrivalsRepositoryImpl
import com.danieljm.delijn.data.repository.VehiclePositionRepositoryImpl
import com.danieljm.delijn.domain.repository.BusRepository
import com.danieljm.delijn.domain.repository.RouteRepository
import com.danieljm.delijn.domain.repository.StopArrivalsRepository
import com.danieljm.delijn.domain.repository.StopRepository
import com.danieljm.delijn.domain.repository.VehiclePositionRepository
import com.danieljm.delijn.domain.usecase.GetBusDetailsUseCase
import com.danieljm.delijn.domain.usecase.GetRouteDetailsUseCase
import com.danieljm.delijn.domain.usecase.GetCachedStopsUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionsSearchUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionDetailUseCase
import com.danieljm.delijn.domain.usecase.GetLineDirectionStopsUseCase
import com.danieljm.delijn.domain.usecase.GetNearbyStopsUseCase
import com.danieljm.delijn.domain.usecase.GetRealTimeArrivalsUseCase
import com.danieljm.delijn.domain.usecase.GetScheduledArrivalsUseCase
import com.danieljm.delijn.domain.usecase.GetStopDetailsUseCase
import com.danieljm.delijn.domain.usecase.GetRealTimeArrivalsForStopUseCase
import com.danieljm.delijn.domain.usecase.GetVehiclePositionUseCase
import com.danieljm.delijn.domain.usecase.SearchStopsUseCase
import com.danieljm.delijn.ui.screens.busdetail.BusDetailViewModel
import com.danieljm.delijn.ui.screens.home.HomeViewModel
import com.danieljm.delijn.ui.screens.routedetail.RouteDetailViewModel
import com.danieljm.delijn.ui.screens.search.SearchViewModel
import com.danieljm.delijn.ui.screens.searchdetail.SearchDetailViewModel
import com.danieljm.delijn.ui.screens.settings.SettingsViewModel
import com.danieljm.delijn.ui.screens.stops.StopsViewModel
import com.danieljm.delijn.ui.screens.stopdetailscreen.StopDetailViewModel
import com.danieljm.delijn.ui.screens.plan.PlanViewModel
import com.danieljm.delijn.ui.screens.plan.PlanRepository
import com.danieljm.delijn.ui.screens.plan.LocationRepository
import com.danieljm.delijn.ui.screens.plan.SettingsRepository
import com.danieljm.delijn.ui.screens.plan.DefaultPlanRepository
import com.danieljm.delijn.ui.screens.plan.DefaultLocationRepository
import com.danieljm.delijn.ui.screens.plan.DefaultSettingsRepository
import com.danieljm.delijn.ui.components.map.MapViewModel
import com.danieljm.delijn.utils.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// New imports for location provider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.danieljm.delijn.data.location.LocationProvider
import com.danieljm.delijn.data.location.LocationProviderImpl

val appModule = module {
    // ViewModel for shared map state
    single { MapViewModel() }

    // Network - provide OkHttpClient with both API keys and Retrofit with proxy base URL
    single { NetworkModule.provideOkHttpClient(Constants.API_KEY, Constants.REALTIME_API_KEY) }
    single { NetworkModule.provideRetrofit(get()) }
    single { NetworkModule.provideApiService(get()) }

    // Database
    single<AppDatabase> { DatabaseModule.provideDatabase(androidContext()) }
    single<StopDao> { get<AppDatabase>().stopDao() }
    single<BusDao> { get<AppDatabase>().busDao() }
    single<RouteDao> { get<AppDatabase>().routeDao() }

    // Repositories
    single<StopRepository> { StopRepositoryImpl(get(), get()) }
    single<BusRepository> { BusRepositoryImpl(get(), get()) }
    single<RouteRepository> { RouteRepositoryImpl(get(), get()) }
    single<StopArrivalsRepository> { StopArrivalsRepositoryImpl(get()) }
    single<VehiclePositionRepository> { VehiclePositionRepositoryImpl(get()) }

    // Plan screen repositories
    single<PlanRepository> { DefaultPlanRepository() }
    single<LocationRepository> { DefaultLocationRepository() }
    single<SettingsRepository> { DefaultSettingsRepository() }

    // Use cases
    single { SearchStopsUseCase(get()) }
    single { GetStopDetailsUseCase(get()) }
    single { GetBusDetailsUseCase(get()) }
    single { GetRouteDetailsUseCase(get()) }
    single { GetRealTimeArrivalsUseCase(get()) }
    single { GetScheduledArrivalsUseCase(get()) }
    single { GetRealTimeArrivalsForStopUseCase(get()) }
    single { GetNearbyStopsUseCase(get()) }
    single { GetCachedStopsUseCase(get()) }
    single { GetLineDirectionsForStopUseCase(get()) }
    // New search use-case for fetching public line number and colors by omschrijving
    single { GetLineDirectionsSearchUseCase(get()) }
    // Use-case to fetch a single lijnrichting detail (contains kleuren and publiek nummer)
    single { GetLineDirectionDetailUseCase(get()) }
    // Use-case to fetch stops (haltes) for a specific lijnrichting to draw route polylines
    single { GetLineDirectionStopsUseCase(get()) }
    // Vehicle position use-case
    single { GetVehiclePositionUseCase(get()) }

    // Provide FusedLocationProviderClient and LocationProvider implementation
    single<FusedLocationProviderClient> { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single<LocationProvider> { LocationProviderImpl(get()) }

    // ViewModels
    viewModel { StopsViewModel(get<GetNearbyStopsUseCase>(), get<GetCachedStopsUseCase>(), get<GetLineDirectionsForStopUseCase>(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { SearchDetailViewModel(get(), get()) }
    viewModel { BusDetailViewModel(get()) }
    viewModel { RouteDetailViewModel(get()) }
    viewModel { HomeViewModel() }
    viewModel { SettingsViewModel() }
    viewModel { StopDetailViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { PlanViewModel(get(), get(), get()) }
}
