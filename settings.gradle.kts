import java.util.Properties
val localProperties = Properties()
val localPropertiesFile = rootDir.resolve("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

val mapboxApiKey: String =
    localProperties.getProperty("MAP_BOX_API_KEY")
        ?: System.getenv("MAP_BOX_API_KEY")
        ?: error("❌ MAP_BOX_API_KEY not found in local.properties or environment variables")


pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = mapboxApiKey
            }
        }
    }
}

rootProject.name = "Livo"
include(":app")
