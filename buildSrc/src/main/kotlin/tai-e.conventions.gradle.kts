plugins {
    id("java")
    id("checkstyle")
}

repositories {
    mavenCentral()
    maven { // repository for Soot snapshot
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

tasks.compileJava { options.encoding = "UTF-8" }
tasks.compileTestJava { options.encoding = "UTF-8" }

// checks the code style after compilation
tasks.classes { finalizedBy("checkstyleMain") }
tasks.testClasses { finalizedBy("checkstyleTest") }

// custom the report format
checkstyle {
    isShowViolations = false
}
tasks.withType<Checkstyle>().configureEach {
    // only xml need
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
    // report the error style to help developers fix it
    doLast { summarizeStyleViolations(this as Checkstyle)?.let(logger::warn) }
}

