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
    // Build release artifacts for smoke tests.
    val distZipTask = rootProject.tasks.named<Zip>("distZip")
    val fatJarTask = rootProject.tasks.named<Jar>("fatJar")
    val docsAllTask = project(":docs").tasks.named<Zip>("all")
    dependsOn(distZipTask, fatJarTask, docsAllTask)
    val distributionZipFile = distZipTask.get().archiveFile.get().asFile
    val fatJarFile = fatJarTask.get().archiveFile.get().asFile
    val docsZipFile = docsAllTask.get().archiveFile.get().asFile

    systemProperty("tai-e.distribution-zip.path", distributionZipFile.absolutePath)
    systemProperty("tai-e.fatjar.path", fatJarFile.absolutePath)
    systemProperty("tai-e.docs-zip.path", docsZipFile.absolutePath)
    systemProperty("tai-e.project.version", projectVersion)
    systemProperty("tai-e.project.snapshot", isSnapshot)
    systemProperty("tai-e.project.path", rootProject.projectDir.absolutePath)
    systemProperty("java.executable.path", javaExecutablePath.asFile.absolutePath)
}
