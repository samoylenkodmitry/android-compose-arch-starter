plugins {
  alias(libs.plugins.android.lib)
  id("org.jetbrains.kotlin.android")
  alias(libs.plugins.hilt)
  id("com.google.devtools.ksp")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.example.feature.settings.impl"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }
}

dependencies {
  implementation(project(":feature:settings:api"))
  implementation(project(":core:common"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.nav.compose)
  implementation(libs.lifecycle.viewmodel.compose)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
