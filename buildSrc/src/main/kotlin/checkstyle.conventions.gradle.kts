plugins {
    checkstyle
}

// checks the code style after compilation
tasks.findByName("classes")?.finalizedBy("checkstyleMain")
tasks.findByName("testClasses")?.finalizedBy("checkstyleTest")

// custom the report format
checkstyle {
    if (System.getenv("CI") == "true") {
        isShowViolations = true
        isIgnoreFailures = false
        maxWarnings = 0
    } else {
        isShowViolations = true
    }
}
tasks.withType<Checkstyle> {
    // only xml need
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
    // report the error style to help developers fix it
    doLast { summarizeStyleViolations(this as Checkstyle)?.let(logger::warn) }
}
