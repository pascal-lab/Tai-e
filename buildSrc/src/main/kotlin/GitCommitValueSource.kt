import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * A ValueSource implementation that retrieves the current Git commit hash from the repository.
 *
 * This implementation reads Git metadata files directly from the `.git` directory to determine
 * the current commit hash. It supports both regular Git repositories and repositories with
 * references stored in the info/refs file.
 *
 * This ValueSource is designed to be compatible with Gradle's Configuration Cache by properly
 * declaring its inputs and avoiding direct file system access during configuration time.
 *
 * @see ValueSource
 */
abstract class GitCommitValueSource
    : ValueSource<String, GitCommitValueSource.Parameters> {

    /**
     * Parameters for the GitCommitValueSource.
     *
     * Contains the configuration needed to locate and read Git metadata files.
     */
    interface Parameters : ValueSourceParameters {

        /**
         * The directory containing `.git` directory.
         */
        val projectDir: DirectoryProperty
    }

    /**
     * Obtains the current Git commit hash by reading Git metadata files.
     *
     * The implementation follows this process:
     * 1. Reads the `.git/HEAD` file to determine the current reference
     * 2. If HEAD points to a branch reference (starts with "ref: "):
     *    - First attempts to read the commit hash from the corresponding file under `.git/refs/`
     *    - If the reference file doesn't exist, falls back to reading from `.git/info/refs`
     * 3. If HEAD contains a direct commit hash, returns it directly
     *
     * @return The current Git commit hash as a String, or "Unknown" if the commit hash
     *         cannot be determined (e.g., not a Git repository, corrupted Git metadata,
     *         or I/O errors)
     */
    override fun obtain(): String {
        return try {
            val projectDir = parameters.projectDir.asFile.get()
            val gitHeadFile = projectDir.resolve(".git/HEAD")
            if (!gitHeadFile.exists()) return "Unknown"

            val gitHead = gitHeadFile.readText().trim()
            if (gitHead.startsWith("ref: ")) {
                val ref = gitHead.substring(5).trim()
                // ref format may be like "refs/heads/master",
                // which indicates we should read the file at '.git/refs/heads/master'
                // to get the actual commit hash for the specified branch
                val refFile = projectDir.resolve(".git/${ref}")
                if (refFile.exists()) {
                    refFile.readText().trim()
                } else {
                    // read from '.git/info/refs' line by line
                    val infoRefsFile = projectDir.resolve(".git/info/refs")
                    if (infoRefsFile.exists()) {
                        infoRefsFile.readLines()
                            .firstOrNull { line -> line.endsWith(ref) }
                            ?.split("\t")
                            ?.get(0) ?: "Unknown"
                    } else {
                        "Unknown"
                    }
                }
            } else {
                // HEAD contains a direct commit hash (detached HEAD state)
                gitHead
            }
        } catch (e: Exception) {
            // Return "Unknown" for any I/O errors or unexpected file formats
            "Unknown"
        }
    }

}
