pluginManagement {
    repositories {
        val takUser = sequenceOf("ARTIFACTORY_CONSUME_USER", "ARTIFACTORY_USER", "TAK_USERNAME")
            .mapNotNull { System.getenv(it)?.takeIf { value -> value.isNotEmpty() } }.firstOrNull()
        val takPass = sequenceOf("ARTIFACTORY_CONSUME_PASSWORD", "ARTIFACTORY_PASSWORD", "TAK_PASSWORD")
            .mapNotNull { System.getenv(it)?.takeIf { value -> value.isNotEmpty() } }.firstOrNull()
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
        // Avoid public-repository rate limits; Plugin Portal can redirect to Maven Central.
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
        val takUser = sequenceOf("ARTIFACTORY_CONSUME_USER", "ARTIFACTORY_USER", "TAK_USERNAME")
            .mapNotNull { System.getenv(it)?.takeIf { value -> value.isNotEmpty() } }.firstOrNull()
        val takPass = sequenceOf("ARTIFACTORY_CONSUME_PASSWORD", "ARTIFACTORY_PASSWORD", "TAK_PASSWORD")
            .mapNotNull { System.getenv(it)?.takeIf { value -> value.isNotEmpty() } }.firstOrNull()
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
        google()
        mavenCentral()
    }
}

rootProject.name = "weartak-plugin-lace"
include(":app")
