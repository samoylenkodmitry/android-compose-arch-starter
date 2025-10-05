plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.archstarter.feature.catalog.impl"
  compileSdk = 35
  defaultConfig { minSdk = 33 }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions { jvmTarget = "21" }
  buildFeatures { compose = true }
}

ksp {
  arg("com.archstarter.di.moduleName", project.path)
  arg("com.archstarter.di.metadataDir", rootProject.layout.buildDirectory.dir("di-metadata").get().asFile.absolutePath)
}

dependencies {
  implementation(project(":feature:catalog:api"))
  implementation(project(":core:common"))
  implementation(project(":core:di-annotations"))
  implementation(project(":feature:settings:api"))
  implementation(project(":feature:settings:impl"))

  implementation(libs.kotlin.inject.runtime)
  ksp(libs.kotlin.inject.compiler)
  ksp(project(":tools:di-processor"))
  implementation(libs.lifecycle.viewmodel.compose)
  api(libs.retrofit.core)
  api(libs.retrofit.kotlinx)
  api(libs.retrofit.scalars)
  api(libs.okhttp.logging)
  api(libs.room.runtime)
  api(libs.room.ktx)
  implementation(libs.kotlinx.serialization.json)
  ksp(libs.room.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
