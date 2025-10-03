package com.archstarter.shared.data

import com.archstarter.shared.cache.AppDatabase
import com.archstarter.shared.cache.DatabaseDriverFactory
import me.tatarka.inject.annotations.Provides

expect class DataModule {
    @Provides
    fun provideDatabase(driverFactory: DatabaseDriverFactory): AppDatabase
}