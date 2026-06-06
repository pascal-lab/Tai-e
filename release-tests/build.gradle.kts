plugins {
    id("java.conventions")
    id("checkstyle.conventions")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // Build the distribution for primary CLI tests and the fat JAR for compatibility tests.
    val installDistTask = rootProject.tasks.named("installDist")
    val fatJarTask = rootProject.tasks.named<Jar>("fatJar")
    dependsOn(installDistTask, fatJarTask)
    val executableName = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "$projectArtifactId.bat"
    } else {
        projectArtifactId
    }
    val executablePath = rootProject.layout.buildDirectory.file(
        "install/$projectArtifactId/bin/$executableName")
    val fatJarFile = fatJarTask.get().archiveFile.get().asFile

    systemProperty("tai-e.fatjar.path", fatJarFile.absolutePath)
    systemProperty("tai-e.executable.path", executablePath.get().asFile.absolutePath)
    systemProperty("tai-e.project.path", rootProject.projectDir.absolutePath)
    systemProperty("java.executable.path", javaExecutablePath.asFile.absolutePath)
}
