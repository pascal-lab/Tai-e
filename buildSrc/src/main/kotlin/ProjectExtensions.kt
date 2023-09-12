import org.gradle.api.Project

fun Project.getProperty(key: String) =
    providers.gradleProperty(key).get()

val Project.projectGroupId: String
    get() = getProperty("projectGroupId")

val Project.projectArtifactId: String
    get() = getProperty("projectArtifactId")

val Project.projectVersion: String
    get() = getProperty("projectVersion")

val Project.projectUrl: String
    get() = getProperty("projectUrl")

val Project.projectDescription: String
    get() = getProperty("projectDescription")

val Project.isSnapshot: Boolean
    get() = projectVersion.endsWith("-SNAPSHOT")
