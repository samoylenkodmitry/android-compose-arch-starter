plugins {
  alias(libs.plugins.android.lib)
  id("org.jetbrains.kotlin.android")
  alias(libs.plugins.hilt)
  id("org.jetbrains.kotlin.kapt")
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

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
