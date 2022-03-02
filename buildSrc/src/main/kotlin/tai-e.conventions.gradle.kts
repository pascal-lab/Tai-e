plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven { // repository for Soot snapshot
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

tasks.compileJava { options.encoding = "UTF-8" }
tasks.compileTestJava { options.encoding = "UTF-8" }
