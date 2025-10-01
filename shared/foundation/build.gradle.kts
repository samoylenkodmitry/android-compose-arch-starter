@file:Suppress("UnstableApiUsage")

import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.api.JavaVersion

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.lib)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
}

kotlin {
  targetHierarchy.default()
  androidTarget()
  jvm("desktop")

  jvmToolchain(21)

  sourceSets {
    val commonMain by getting {
      kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
      dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlin.inject.runtime.kmp)
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

dependencies {
  add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
  add("kspAndroid", libs.kotlin.inject.compiler)
  add("kspDesktop", libs.kotlin.inject.compiler)
}

tasks.withType<KspAATask>().configureEach {
  if (name != "kspCommonMainKotlinMetadata") {
    dependsOn("kspCommonMainKotlinMetadata")
  }
}
