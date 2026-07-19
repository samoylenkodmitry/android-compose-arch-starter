import org.gradle.api.JavaVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.lib)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
}

kotlin {
  targetHierarchy.default()
  androidTarget()
  jvm("desktop")

  jvmToolchain(21)

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
  }
}

android {
  namespace = "com.archstarter.shared.foundation"
  compileSdk = 35
  defaultConfig { minSdk = 33 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}
