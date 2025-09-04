plugins {
    checkstyle
}

// checks the code style after compilation
tasks.findByName("classes")?.finalizedBy("checkstyleMain")
tasks.findByName("testClasses")?.finalizedBy("checkstyleTest")

// custom the report format
checkstyle {
    isShowViolations = false
    if (System.getenv("CI") == "true") {
        isIgnoreFailures = false
        maxWarnings = 0
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
