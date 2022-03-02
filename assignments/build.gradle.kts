plugins {
    id("java")
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("lib/dependencies.jar"))
    testImplementation("junit:junit:4.13")
}

application {
    mainClass.set("pascal.taie.Assignment")
}

tasks.compileJava { options.encoding = "UTF-8" }
tasks.compileTestJava { options.encoding = "UTF-8" }

tasks.test {
    useJUnit()
    maxHeapSize = "4G"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
