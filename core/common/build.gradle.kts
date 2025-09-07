plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.kotlin.compose)
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.compose.runtime)
  implementation(libs.javax.inject)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
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
