plugins {
  // empty at root
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}
