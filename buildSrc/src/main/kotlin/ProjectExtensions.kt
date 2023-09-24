import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

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

val Project.javaVersion: JavaLanguageVersion
    get() = JavaLanguageVersion.of(17)

val Project.javaExecutablePath: RegularFile
    get() = (this as ProjectInternal).services.get(JavaToolchainService::class.java)
        .launcherFor { languageVersion.set(javaVersion) }
        .map { it.executablePath }
        .get()
