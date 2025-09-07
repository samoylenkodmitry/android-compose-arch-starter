plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.kotlin.kapt)
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.javax.inject)
  implementation(libs.dagger)
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
