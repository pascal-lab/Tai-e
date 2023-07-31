import java.nio.file.Files

plugins {
    java
    checkstyle
    jacoco
}

repositories {
    mavenCentral()
}

tasks.compileJava { options.encoding = "UTF-8" }
tasks.compileTestJava { options.encoding = "UTF-8" }

// checks the code style after compilation
tasks.classes { finalizedBy("checkstyleMain") }
tasks.testClasses { finalizedBy("checkstyleTest") }

tasks.withType<Javadoc> {
    val javadocConfigDir = rootProject.rootDir.resolve(
        "config/javadoc/")
    fun String.escapeLineBreaking() = this.replace("\r", "")
        .replace("\n", "")
    options {
        this as StandardJavadocDocletOptions
        // style
        title = "Tai-e API"
        overview = javadocConfigDir.resolve("overview.html").path
        addStringOption("-add-stylesheet",
            javadocConfigDir.resolve("javadoc.css").path)
        header = Files.readString(javadocConfigDir.resolve("header.html").toPath())
            .escapeLineBreaking()
        bottom = Files.readString(javadocConfigDir.resolve("footer.html").toPath())
            .escapeLineBreaking()
        // fix language and encoding
        encoding = "UTF-8"
        docEncoding = "UTF-8"
        locale = "en"
        jFlags("-Duser.language=en")
        // suppress the warning(s)
        addBooleanOption("Xdoclint:all,-missing", true)
        // others
        splitIndex(true)
        use(true)
        noTimestamp(true)
        memberLevel = JavadocMemberLevel.PROTECTED
        addBooleanOption("html5", true)
        links("https://docs.oracle.com/en/java/javase/17/docs/api")
    }
}

// custom the report format
checkstyle {
    isShowViolations = false
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

// jacoco
tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
