pluginManagement {
    repositories {
        // Runner pool jobs share one egress IP that Maven Central rate-limits (HTTP 429),
        // and Gradle aborts on a 429 instead of trying the next repository.
        val takUser = listOf("ARTIFACTORY_CONSUME_USER", "ARTIFACTORY_USER", "TAK_USERNAME")
            .firstNotNullOfOrNull { System.getenv(it)?.takeIf(String::isNotBlank) }
        val takPass = listOf("ARTIFACTORY_CONSUME_PASSWORD", "ARTIFACTORY_PASSWORD", "TAK_PASSWORD")
            .firstNotNullOfOrNull { System.getenv(it)?.takeIf(String::isNotBlank) }
        if (takUser != null && takPass != null) {
            maven {
                name = "takArtifactory"
                url = uri("https://artifacts.tak.gov/artifactory/maven-release")
                credentials {
                    username = takUser
                    password = takPass
                }
            }
        }
        println("takArtifactory for plugins: ${if (takUser != null && takPass != null) "enabled" else "disabled (no credentials in environment)"}")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// pluginManagement is compiled before the rest of this script and cannot see these values,
// so it repeats them above.
val takArtifactoryUrl = "https://artifacts.tak.gov/artifactory/maven-release"
val takUser = listOf("ARTIFACTORY_CONSUME_USER", "ARTIFACTORY_USER", "TAK_USERNAME")
    .firstNotNullOfOrNull { System.getenv(it)?.takeIf(String::isNotBlank) }
val takPass = listOf("ARTIFACTORY_CONSUME_PASSWORD", "ARTIFACTORY_PASSWORD", "TAK_PASSWORD")
    .firstNotNullOfOrNull { System.getenv(it)?.takeIf(String::isNotBlank) }
if (takUser != null && takPass != null) {
    // Read by app/build.gradle.kts for Robolectric, which downloads outside Gradle's repositories.
    gradle.extra["takArtifactoryUrl"] = takArtifactoryUrl
    gradle.extra["takArtifactoryUser"] = takUser
    gradle.extra["takArtifactoryPassword"] = takPass
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (takUser != null && takPass != null) {
            maven {
                name = "takArtifactory"
                url = uri(takArtifactoryUrl)
                credentials {
                    username = takUser
                    password = takPass
                }
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "weartak-plugin-lace"
include(":app")
