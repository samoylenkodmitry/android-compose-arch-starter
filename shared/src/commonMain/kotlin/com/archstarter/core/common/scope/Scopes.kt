package com.archstarter.core.common.scope

import me.tatarka.inject.annotations.Scope

@Scope
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Scope
@Retention(AnnotationRetention.BINARY)
annotation class ScreenScope