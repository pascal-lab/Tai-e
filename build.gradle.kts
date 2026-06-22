plugins {
    application
    id("java.conventions")
    id("checkstyle.conventions")
    id("maven-publish.conventions")
}

group = projectGroupId
description = projectArtifactId
version = projectVersion

dependencies {
    // Process options
    implementation("info.picocli:picocli:4.7.7")
    // Logger
    val log4jVersion = "2.24.3"
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    // Process YAML configuration files
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.0")
    // Use Soot as frontend
    implementation(files("lib/sootclasses-modified.jar"))
    "org.soot-oss:soot:4.4.1".let {
        // Disable transitive dependencies from Soot in compile classpath
        compileOnly(it) { isTransitive = false }
        testCompileOnly(it) { isTransitive = false }
        runtimeOnly(it)
    }
    // JSR305, for javax.annotation
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    // Use asm to read class file
    val asmVersion = "9.8"
    implementation("org.ow2.asm:asm:$asmVersion")
    implementation("org.ow2.asm:asm-commons:$asmVersion")
    implementation("org.ow2.asm:asm-tree:$asmVersion")
    implementation("org.ow2.asm:asm-util:$asmVersion")
    // Use FlowDroid to parse AXML files
    implementation(files("lib/flowdroidclasses-modified.jar"))
    "de.fraunhofer.sit.sse.flowdroid:soot-infoflow-android:2.14.1".let {
        // Disable transitive dependencies from FlowDroid in compile classpath
        compileOnly(it) { isTransitive = false }
        testCompileOnly(it) { isTransitive = false }
        runtimeOnly(it) {
            exclude(group = "org.soot-oss", module = "soot")
            exclude(group = "org.slf4j", module = "slf4j-simple")
        }
    }
    implementation("de.upb.cs.swt:axml:2.1.3")


    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
}

application {
    mainClass.set("pascal.taie.Main")
    applicationName = projectArtifactId
}

val apiDocs = tasks.named("javadoc")
val docsProject = project(":docs")

distributions {
    main {
        distributionBaseName.set(projectArtifactId)
        contents {
            from(files("COPYING", "COPYING.LESSER",
                "README.md", "CHANGELOG.md", "CITATION.bib"))
            from(apiDocs) {
                into("docs/api")
            }
        }
    }
}

docsProject.plugins.withId("org.asciidoctor.jvm.convert") {
    val referenceDocs = docsProject.files(
        docsProject.layout.buildDirectory.dir("docs/asciidoc")
    ).builtBy(docsProject.tasks.named("asciidoctor"))

    distributions {
        main {
            contents {
                from(referenceDocs) {
                    into("docs/reference")
                }
            }
        }
    }
}

tasks.startScripts {
    applicationName = projectArtifactId
}

tasks.register("generateBuildInfo") {
    group = "build"
    description = "Generates build information properties file"
    // write tai-e build information into META-INF/tai-e-build.properties
    val buildPropsFile = rootProject.layout.buildDirectory.file(
        "resources/main/META-INF/tai-e-build.properties").get().asFile
    val versionProvider = projectVersionProvider
    val commitProvider = projectCommitProvider
    doFirst {
        buildPropsFile.parentFile.mkdirs()
        val buildProps = """
            version=${versionProvider.get()}
            commit=${commitProvider.get()}
        """.trimIndent()
        buildPropsFile.writeText(buildProps)
    }
}

tasks.register<Jar>("fatJar", Jar::class) {
    group = "build"
    description = "Creates a legacy single jar file including Tai-e and all dependencies"
    manifest {
        attributes["Main-Class"] = "pascal.taie.Main"
    }
    archiveBaseName.set("${projectArtifactId}-all")
    from(
        configurations.runtimeClasspath.get().map { file ->
            if (file.isDirectory) file else zipTree(file).matching {
                exclude("META-INF/**/*.RSA")
            }
        }
    )
    from("COPYING", "COPYING.LESSER")
    destinationDirectory.set(rootProject.layout.buildDirectory)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    with(tasks["jar"] as CopySpec)
}

tasks.jar {
    from("COPYING", "COPYING.LESSER")
    destinationDirectory.set(rootProject.layout.buildDirectory)
    archiveBaseName.set(projectArtifactId)
}

tasks.processResources {
    // Generate a build information properties file in resources directory,
    // so that it can be included in the class path and JAR file.
    finalizedBy("generateBuildInfo")
}

tasks.withType<Test> {
    // Uses JUnit5
    useJUnitPlatform()
    // Increases the maximum heap memory of JUnit test process. The default is 512M.
    // (see org.gradle.process.internal.worker.DefaultWorkerProcessBuilder.build)
    maxHeapSize = "2G"
    // Sets the maximum number of test processes to start in parallel.
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    // Sets the default classpath for test execution.
    // (see https://docs.gradle.org/current/userguide/upgrading_version_8.html#test_task_default_classpath)
    val test by testing.suites.existing(JvmTestSuite::class)
    testClassesDirs = files(test.map { it.sources.output.classesDirs })
    classpath = files(test.map { it.sources.runtimeClasspath })
}

tasks.test {
    doFirst {
        // Exclude TestSuite classes when running all tests to prevent duplicate execution
        // Only apply this filter when no specific test patterns are specified via command line
        val testFilter = filter as? org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
        val hasCommandLineIncludePatterns = testFilter
            ?.commandLineIncludePatterns
            ?.isNotEmpty()
            ?: false
        if (!hasCommandLineIncludePatterns) {
            filter {
                excludeTestsMatching("*TestSuite")
            }
        }
    }
}
