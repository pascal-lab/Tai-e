plugins {
    id("org.asciidoctor.jvm.convert") version "4.0.4"
    id("org.asciidoctor.jvm.pdf") version "4.0.4"
}

repositories {
    mavenCentral()
}

version = projectVersion

val asciidoctorExtensions: Configuration by configurations.creating

dependencies {
    asciidoctorExtensions("io.spring.asciidoctor.backends:spring-asciidoctor-backends:0.0.7")
}

tasks.withType(org.asciidoctor.gradle.jvm.AbstractAsciidoctorTask::class) {
    // set source directory to docs/ instead of docs/src/docs/asciidoc/
    sourceDir(sourceDir.parentFile.parentFile.parentFile)
    // suppress the warning 'Native subprocess control requires open access to the JDK IO subsystem'
    // until https://github.com/jruby/jruby/issues/6721
    jvm {
        jvmArgs(listOf(
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.base/java.io=ALL-UNNAMED",
        ))
        // use the specified toolchain instead of the Gradle's JVM
        executable(javaExecutablePath)
    }
    // i18n
    languages("en")
    // others
    baseDirFollowsSourceFile()
}

tasks.named("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {
    // apply the spring-html backend
    configurations("asciidoctorExtensions")
    outputOptions {
        backends("spring-html")
    }
    // customize banner and background
    doLast {
        backendOutputDirectories.forEach { dir ->
            listOf(
                "img/banner-logo.svg",
                "img/doc-background.svg",
                "img/doc-background-dark.svg"
            ).forEach { dir.resolve(it).delete() }
        }
        copy {
            from(sourceDir)
            include("common/**")
            into(outputDir)
        }
    }
}

tasks.named("asciidoctorPdf", org.asciidoctor.gradle.jvm.pdf.AsciidoctorPdfTask::class) {
    asciidoctorj {
        sources { include("index-single.adoc") }
    }
}

tasks.register<Zip>("all", Zip::class) {
    group = "documentation"
    description = "Builds all documentation"
    archiveFileName.set("tai-e-docs.zip")
    destinationDirectory.set(layout.buildDirectory)
    // The generated documentation contains timestamps, which prevents
    // Gradle 9.0.0's default reproducible archive feature from working.
    // We preserve these timestamps to maintain clarity about generation time.
    isPreserveFileTimestamps = true
    dependsOn(":docs:asciidoctor", ":javadoc")
    from(layout.buildDirectory.dir("docs/asciidoc")) {
        into("$projectVersion/reference")
    }
    from(rootProject.layout.buildDirectory.dir("docs/javadoc")) {
        into("$projectVersion/api")
    }
    if (isSnapshot) {
        from(layout.buildDirectory.dir("docs/asciidoc")) {
            into("current/reference")
        }
        from(rootProject.layout.buildDirectory.dir("docs/javadoc")) {
            into("current/api")
        }
    }
}
