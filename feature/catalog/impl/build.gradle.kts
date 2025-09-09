plugins {
  alias(libs.plugins.android.lib)
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
  alias(libs.plugins.kotlin.serialization)
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.example.feature.catalog.impl"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }
  buildFeatures { compose = true }
}

dependencies {
  implementation(project(":feature:catalog:api"))
  implementation(project(":core:common"))

  implementation(libs.koin.android)
  implementation(libs.koin.androidx.compose)
  implementation(libs.lifecycle.viewmodel.compose)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.kotlinx)
  implementation(libs.retrofit.scalars)
  implementation(libs.okhttp.logging)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  implementation(libs.kotlinx.serialization.json)
  ksp(libs.room.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
