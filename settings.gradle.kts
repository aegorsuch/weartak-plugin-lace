pluginManagement {
    repositories {
        // Runner pool jobs share one egress IP that Maven Central rate-limits (HTTP 429),
        // and Gradle aborts on a 429 instead of trying the next repository.
        val takUser = System.getenv("ARTIFACTORY_CONSUME_USER") ?: System.getenv("ARTIFACTORY_USER") ?: System.getenv("TAK_USERNAME")
        val takPass = System.getenv("ARTIFACTORY_CONSUME_PASSWORD") ?: System.getenv("ARTIFACTORY_PASSWORD") ?: System.getenv("TAK_PASSWORD")
        if (!takUser.isNullOrBlank() && !takPass.isNullOrBlank()) {
            maven {
                name = "takArtifactory"
                url = uri("https://artifacts.tak.gov/artifactory/maven-release")
                credentials {
                    username = takUser
                    password = takPass
                }
            }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val takUser = System.getenv("ARTIFACTORY_CONSUME_USER") ?: System.getenv("ARTIFACTORY_USER") ?: System.getenv("TAK_USERNAME")
        val takPass = System.getenv("ARTIFACTORY_CONSUME_PASSWORD") ?: System.getenv("ARTIFACTORY_PASSWORD") ?: System.getenv("TAK_PASSWORD")
        if (!takUser.isNullOrBlank() && !takPass.isNullOrBlank()) {
            maven {
                name = "takArtifactory"
                url = uri("https://artifacts.tak.gov/artifactory/maven-release")
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
