plugins {
  alias(libs.plugins.android.lib)
  id("org.jetbrains.kotlin.android")
  alias(libs.plugins.hilt)
  id("org.jetbrains.kotlin.kapt")
  alias(libs.plugins.kotlin.serialization)
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
}

dependencies {
  implementation(project(":feature:catalog:api"))
  implementation(project(":core:common"))

  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)
  implementation(libs.lifecycle.viewmodel.compose)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.kotlinx)
  implementation(libs.retrofit.scalars)
  implementation(libs.okhttp.logging)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  implementation(libs.kotlinx.serialization.json)
  kapt(libs.room.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
