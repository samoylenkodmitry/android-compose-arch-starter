package com.archstarter.app

import android.content.Context
import com.archstarter.core.common.app.App

class AppScopeManager {
    private var component: AppComponent? = null

    fun create(context: Context, app: App): AppComponent {
        check(component == null) { "App scope already active" }
        val created = AppComponent::class.create(context, app)
        component = created
        return created
    }

    fun component(): AppComponent = component ?: error("App scope is not initialized")

    fun clear() {
        component = null
    }
}
