plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.hilt)
  alias(libs.plugins.kotlin.kapt)
}

android {
  namespace = "com.example.feature.detail.impl"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation(project(":feature:detail:api"))
  implementation(project(":core:common"))

  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)
  implementation(libs.lifecycle.viewmodel.compose)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
