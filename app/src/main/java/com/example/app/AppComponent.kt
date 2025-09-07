package com.example.app

import com.example.core.common.app.App
import com.example.core.common.app.AppScope
import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.components.SingletonComponent

@AppScope
@DefineComponent(parent = SingletonComponent::class)
interface AppComponent {
    @DefineComponent.Builder
    interface Builder {
        fun app(@BindsInstance app: App): Builder
        fun build(): AppComponent
    }
}
