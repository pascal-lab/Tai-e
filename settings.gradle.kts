// rootProject.name = "Tai-e" // Mismatch between project name and folder name may cause Intellij error

include(
    ":", // root project
    "docs",
)

// Auto detect and provision JVM
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}
