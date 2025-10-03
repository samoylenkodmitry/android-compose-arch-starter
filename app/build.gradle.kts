plugins {
  alias(libs.plugins.android.app)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.archstarter.app"
  compileSdk = 35
  defaultConfig {
    applicationId = "com.archstarter.app"
    minSdk = 33
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }
  packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }

  signingConfigs {
    getByName("debug")
  }
  buildTypes {
    getByName("release") {
      signingConfig = signingConfigs.getByName("debug")
    }
  }

}

dependencies {
  implementation(project(":shared"))
  implementation(libs.activity.compose)
  implementation(libs.compose.ui)
  implementation(libs.compose.material3)
  implementation(libs.compose.preview)
  debugImplementation(libs.compose.tooling)

  implementation(libs.lifecycle.runtime.compose)
  implementation(libs.lifecycle.viewmodel.compose)
  implementation(libs.navigation.compose)

  androidTestImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.test.manifest)
  androidTestImplementation(libs.test.ext.junit)
  androidTestImplementation(libs.test.espresso.core)
}
