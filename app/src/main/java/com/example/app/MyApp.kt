package com.example.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.example.feature.catalog.impl.data.catalogModule
import com.example.feature.detail.impl.detailModule
import com.example.feature.settings.impl.settingsModule

class MyApp : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@MyApp)
      modules(catalogModule, detailModule, settingsModule)
    }
  }
}
