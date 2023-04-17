import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import org.gradle.api.plugins.quality.Checkstyle

/**
 * Summarizes checkstyle errors.
 *
 * @author Teng Zhang
 */
fun summarizeStyleViolations(checkstyle: Checkstyle): String? {
    data class StyleViolation(val msg: String, val source: String, val path: String)

    fun String.removeHtmlTag(): String = this.replace(Regex("<(.*)>"), "'$1'")

    val violations = mutableListOf<StyleViolation>()
    val reportXml = XmlParser().parse(checkstyle.reports.xml.outputLocation.asFile.get())
    (reportXml.value() as NodeList).forEach {
        it as Node
        val filePath = it.attribute("name") as String
        (it.value() as NodeList).forEach {
            val attributes = (it as Node).attributes()
            val source = attributes["source"] as String
            val errorMsg = attributes["message"] as String
            val errorPath = (filePath
                    + (attributes["line"]?.let { ":$it" } ?: "")
                    + (attributes["column"]?.let { ":$it" } ?: ""))
            violations.add(StyleViolation(errorMsg, source, errorPath))
        }
    }
    // report these violations by sorting them by type in decreasing order of frequency.
    if (violations.size > 0) {
        val violationGroup = violations
                .groupBy { it.source }
                .values
                .sortedByDescending { it.size }
        val result = StringBuilder("Detected style violation(s) that impact code quality.")
        var id = 1
        violationGroup.forEach {
            it.forEach {
                result.append("\n#${id++}: ${it.msg}\n  rule: ${it.source}\n  Please click to fix: ${it.path}")
            }
        }
        return result.toString().removeHtmlTag()
    }
    return null
}
