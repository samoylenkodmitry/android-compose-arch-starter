package com.archstarter.shared.data

import com.archstarter.shared.cache.AppDatabase
import com.archstarter.shared.cache.DatabaseDriverFactory
import me.tatarka.inject.annotations.Provides

actual class DataModule {
    @Provides
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory = DatabaseDriverFactory()

    @Provides
    actual fun provideDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
        return AppDatabase(driverFactory.createDriver())
    }
}