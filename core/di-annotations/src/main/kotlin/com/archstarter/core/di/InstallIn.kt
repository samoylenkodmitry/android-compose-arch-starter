package com.archstarter.core.di

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class InstallInAppComponent

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class InstallInScreenComponent

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class InstallInSubscreenComponent
