plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.feature.detail.ui"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.get() }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation(project(":feature:detail:api"))
  implementation(project(":core:designsystem"))
  implementation(project(":core:common"))

  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.preview)
  implementation(libs.lifecycle.runtime.compose)
  debugImplementation(libs.compose.tooling)
}
