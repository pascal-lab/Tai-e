plugins {
    java
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
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
                "https://s01.oss.sonatype.org/content/repositories/snapshots"
            } else {
                "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
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
