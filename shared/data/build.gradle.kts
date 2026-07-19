import org.gradle.api.JavaVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.lib)
}

kotlin {
  targetHierarchy.default()
  androidTarget()
  jvm("desktop")

  jvmToolchain(21)

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
      }
    }
    val commonTest by getting {
      dependencies { implementation(kotlin("test")) }
    }
  }
}

android {
  namespace = "com.archstarter.shared.data"
  compileSdk = 35
  defaultConfig { minSdk = 33 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}
