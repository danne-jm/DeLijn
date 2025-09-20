package com.danieljm.bussin.di

import com.danieljm.bussin.data.repository.BussinRepositoryImpl
import com.danieljm.bussin.domain.repository.BussinRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBussinRepository(impl: BussinRepositoryImpl): BussinRepository
}

