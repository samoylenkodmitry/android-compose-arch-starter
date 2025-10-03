package com.archstarter.shared.data

import android.content.Context
import com.archstarter.shared.cache.AppDatabase
import com.archstarter.shared.cache.DatabaseDriverFactory
import me.tatarka.inject.annotations.Provides

actual class DataModule(private val context: Context) {
    @Provides
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory = DatabaseDriverFactory(context)

    @Provides
    actual fun provideDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
        return AppDatabase(driverFactory.createDriver())
    }
}