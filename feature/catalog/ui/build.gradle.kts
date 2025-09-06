plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "com.example.feature.catalog.ui"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.get() }
}

dependencies {
  implementation(project(":feature:catalog:api"))
  implementation(project(":core:designsystem"))

  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.preview)
  implementation(libs.lifecycle.runtime.compose)
  debugImplementation(libs.compose.tooling)
}
