import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

plugins {
    java
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Javadoc> {
    val javadocConfigDir = rootProject.rootDir.resolve(
        "config/javadoc/")
    fun String.escapeLineBreaking() = this.replace("\r", "")
        .replace("\n", "")
    options {
        this as StandardJavadocDocletOptions
        // style
        title = "Tai-e $projectVersion API"
        overview = javadocConfigDir.resolve("overview.html").path
        addStringOption("-add-stylesheet",
            javadocConfigDir.resolve("javadoc.css").path)
        header = javadocConfigDir.resolve("header.html").readText().escapeLineBreaking()
        bottom = javadocConfigDir.resolve("footer.html").readText().escapeLineBreaking()
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

val signingKeyId: String? by project         // env.ORG_GRADLE_PROJECT_signingKeyId
val signingKey: String? by project           // env.ORG_GRADLE_PROJECT_signingKey
val signingPassword: String? by project      // env.ORG_GRADLE_PROJECT_signingPassword
val mavenCentralUsername: String? by project // env.ORG_GRADLE_PROJECT_mavenCentralUsername
val mavenCentralPassword: String? by project // env.ORG_GRADLE_PROJECT_mavenCentralPassword

publishing {
    if (mavenCentralUsername != null && mavenCentralPassword != null) {
        repositories {
            val repoUrl = if (isSnapshot) {
                "https://central.sonatype.com/repository/maven-snapshots/"
            } else {
                "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            }
            maven {
                name = "Sonatype"
                url = uri(repoUrl)
                credentials {
                    username = mavenCentralUsername
                    password = mavenCentralPassword
                }
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            // components
            from(components["java"])
            // https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:resolved_dependencies
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            // metadata
            groupId = projectGroupId
            version = projectVersion
            artifactId = projectArtifactId
            pom {
                name.set(projectArtifactId)
                description.set(projectDescription)
                url.set(projectUrl)
                licenses {
                    license {
                        name.set("GNU Lesser General Public License, Version 3.0")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.en.html")
                    }
                    license {
                        name.set("GNU General Public License, Version 3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                }
                developers {
                    developer {
                        id.set("silverbullettt")
                        name.set("Tian Tan")
                        email.set("tiantan@nju.edu.cn")
                    }
                    developer {
                        id.set("yuelee")
                        name.set("Yue Li")
                        email.set("yueli@nju.edu.cn")
                    }
                }
                scm {
                    connection.set("scm:git:git://${projectUrl.removePrefix("https://")}.git")
                    developerConnection.set("scm:git:ssh://${projectUrl.removePrefix("https://")}.git")
                    url.set(projectUrl)
                }
            }
        }
    }
}

signing {
    isRequired = !isSnapshot // Gradle Module Metadata currently does not support signing snapshots
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

tasks.named("publish") {
    doLast {
        if (!isSnapshot && mavenCentralUsername != null && mavenCentralPassword != null) {
            uploadToPortal(mavenCentralUsername!!, mavenCentralPassword!!)
        }
    }
}

fun uploadToPortal(username: String, password: String) {
    val baseUrl = "https://ossrh-staging-api.central.sonatype.com"
    val credentials = Base64.getEncoder()
        .encodeToString("$username:$password".toByteArray())
    val client = HttpClient.newHttpClient()

    fun sendReq(
        url: String,
        method: String = "GET",
        body: String = ""
    ): HttpResponse<String> {
        val req = HttpRequest.newBuilder()
            .uri(`java.net`.URI(url))
            .header("Authorization", "Bearer $credentials")
            .header("Content-Type", "application/json")
            .method(method, if (body.isEmpty())
                HttpRequest.BodyPublishers.noBody()
            else HttpRequest.BodyPublishers.ofString(body))
            .build()
        val respBodyHandler = HttpResponse.BodyHandlers.ofString()
        return client.send(req, respBodyHandler)
    }

    try {
        // Search open repositories
        val searchResp = sendReq("$baseUrl/manual/search/repositories?state=open&ip=client")
        if (searchResp.statusCode() != 200) {
            println("Failed to search repositories, status code: ${searchResp.statusCode()}")
            return
        }

        // Extract and close repositories
        """"key"\s*:\s*"([^"]+)"""".toRegex()
            .findAll(searchResp.body())
            .forEach { match ->
                val repoKey = match.groupValues[1]
                sendReq("$baseUrl/manual/upload/repository/$repoKey?publishing_type=user_managed",
                    "POST", "{}")
                println("Upload staging repository to Maven Central Portal Deployments successfully")
            }
    } catch (e: Exception) {
        println("Failed to upload staging repositories: ${e.message}")
    }
}
