plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.kotlin.compose)
}

dependencies {
  implementation(project(":core:common"))
  implementation(libs.compose.runtime)
  implementation(libs.kotlinx.coroutines.core)
}

kotlin {
  jvmToolchain(17)
}
