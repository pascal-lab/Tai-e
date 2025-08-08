// rootProject.name = "Tai-e" // Mismatch between project name and folder name may cause Intellij error

include(
    ":", // root project
    "docs",
    "integration-tests",
)

// Auto detect and provision JVM
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}
