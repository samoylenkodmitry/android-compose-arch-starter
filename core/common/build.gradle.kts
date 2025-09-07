plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.kotlin.kapt)
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation("javax.inject:javax.inject:1")
  implementation("com.google.dagger:dagger:2.52")
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  kapt(libs.hilt.compiler)
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
  }
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(21)
}
