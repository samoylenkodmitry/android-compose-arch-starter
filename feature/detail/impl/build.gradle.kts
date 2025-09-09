plugins {
  alias(libs.plugins.android.lib)
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.example.feature.detail.impl"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }
}

dependencies {
  implementation(project(":feature:detail:api"))
  implementation(project(":feature:catalog:impl"))
  implementation(project(":core:common"))

  implementation(libs.koin.android)
  implementation(libs.koin.androidx.compose)
  implementation(libs.lifecycle.viewmodel.compose)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
