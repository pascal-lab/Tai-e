plugins {
    java
    jacoco
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(javaVersion)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// jacoco
tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
