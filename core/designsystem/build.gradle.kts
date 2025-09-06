plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "com.example.core.designsystem"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.get() }
}

dependencies {
  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.preview)
  debugImplementation(libs.compose.tooling)
}
