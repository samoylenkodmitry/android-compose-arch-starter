plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.archstarter.feature.detail.impl"
  compileSdk = 35
  defaultConfig { minSdk = 33 }
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
  implementation(project(":feature:settings:api"))
  implementation(project(":feature:settings:impl"))

  implementation(libs.kotlin.inject.runtime)
  ksp(libs.kotlin.inject.compiler)
  implementation(libs.lifecycle.viewmodel.compose)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
