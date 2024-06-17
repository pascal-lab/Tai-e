import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import kotlin.io.path.Path
import kotlin.io.path.readText

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

val Project.projectCommit: String
    get() {
        try {
            val gitHead = Path(rootDir.path, ".git", "HEAD").readText()
            if (gitHead.startsWith("ref: ")) {
                return Path(rootDir.path, ".git", gitHead.substring(5).trim()).readText().trim()
            } else {
                return gitHead.trim()
            }
        } catch (e: Exception) {
            logger.warn("Failed to read Git commit hash: {}", e.toString())
        }
        return "Unknown"
    }

val Project.isSnapshot: Boolean
    get() = projectVersion.endsWith("-SNAPSHOT")

val Project.javaVersion: JavaLanguageVersion
    get() = JavaLanguageVersion.of(17)

val Project.javaExecutablePath: RegularFile
    get() = (this as ProjectInternal).services.get(JavaToolchainService::class.java)
        .launcherFor { languageVersion.set(javaVersion) }
        .map { it.executablePath }
        .get()
