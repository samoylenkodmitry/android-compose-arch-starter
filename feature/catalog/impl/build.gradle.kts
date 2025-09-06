plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.hilt)
  kotlin("kapt")
}

android {
  namespace = "com.example.feature.catalog.impl"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
}

dependencies {
  implementation(project(":feature:catalog:api"))
  implementation(project(":core:common"))

  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)
  implementation(libs.lifecycle.viewmodel.compose)
}
