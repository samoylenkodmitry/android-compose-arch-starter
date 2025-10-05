plugins {
  alias(libs.plugins.android.lib)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.archstarter.feature.onboarding.impl"
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
  implementation(project(":feature:onboarding:api"))
  implementation(project(":core:common"))
  implementation(project(":core:di-annotations"))

  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlin.inject.runtime)
  ksp(libs.kotlin.inject.compiler)
  ksp(project(":tools:di-processor"))
  implementation(libs.lifecycle.viewmodel.compose)
}
