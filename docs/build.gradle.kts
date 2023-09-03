plugins {
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("org.asciidoctor.jvm.pdf") version "3.3.2"
}

repositories {
    mavenCentral()
}

tasks.withType(org.asciidoctor.gradle.jvm.AbstractAsciidoctorTask::class) {
    // set source directory to docs/ instead of docs/src/docs/asciidoc/
    sourceDir(sourceDir.parentFile.parentFile.parentFile)
    // Suppress the warning 'Native subprocess control requires open access to the JDK IO subsystem'
    // until https://github.com/jruby/jruby/issues/6721
    forkOptions {
        jvmArgs(listOf(
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.base/java.io=ALL-UNNAMED",
        ))
    }
    // i18n
    languages("en")
    // others
    baseDirFollowsSourceFile()
}

tasks.named("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {
    copy {
        from(sourceDir)
        include("common/**")
        into(outputDir)
    }
}

tasks.named("asciidoctorPdf", org.asciidoctor.gradle.jvm.pdf.AsciidoctorPdfTask::class) {
    asciidoctorj {
        sources { include("index-single.adoc") }
    }
}
