package com.archstarter.app

import android.app.Application

class MyApp : Application() {
    val appScopeManager: AppScopeManager by lazy { AppScopeManager() }
}
