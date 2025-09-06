pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}
rootProject.name = "android-compose-arch-starter"

include(
  ":app",
  ":core:designsystem",
  ":core:common",
  ":feature:catalog:api",
  ":feature:catalog:ui",
  ":feature:catalog:impl"
)
