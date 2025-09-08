plugins {
  alias(libs.plugins.android.lib)
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.example.feature.settings.ui"
  compileSdk = 35
  defaultConfig { minSdk = 24 }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.get() }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }
}

dependencies {
  implementation(project(":feature:settings:api"))
  implementation(project(":core:designsystem"))
  implementation(project(":core:common"))

  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.preview)
  implementation(libs.lifecycle.runtime.compose)
  debugImplementation(libs.compose.tooling)
}
