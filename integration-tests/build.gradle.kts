plugins {
    id("java.conventions")
    id("checkstyle.conventions")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Build fat jar before running tests
    val fatJarTask = rootProject.tasks["fatJar"] as Jar
    dependsOn(fatJarTask)
    val fatJarFile = fatJarTask.archiveFile.get().asFile
    // Set system properties for test runtime
    systemProperty("tai-e.jar.path", fatJarFile.absolutePath)
    systemProperty("tai-e.project.path", rootProject.projectDir.absolutePath)
    systemProperty("java.executable.path", javaExecutablePath.asFile.absolutePath)
}

