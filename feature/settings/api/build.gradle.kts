plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(project(":core:common"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)
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
