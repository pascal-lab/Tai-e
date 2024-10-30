import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.forEachLine
import kotlin.io.path.readText
import kotlin.io.path.useLines

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
                val ref = gitHead.substring(5).trim()
                // path '.git/refs/heads/branchName'
                val p = Path(rootDir.path, ".git", ref)
                if (p.exists()) {
                    return p.readText().trim()
                } else {
                    // read from '.git/info/refs' line by line
                    Path(rootDir.path, ".git", "info", "refs").forEachLine {
                        if (it.endsWith(ref)) {
                            return it.split("\t")[0]
                        }
                    }
                }
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
