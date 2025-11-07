// In settings.gradle.kts
import org.gradle.authentication.http.BasicAuthentication

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
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Mapbox" // Good practice
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { // Authentication block first
                create<BasicAuthentication>("basic")
            }
            credentials { // Credentials block second
                username = "mapbox"
                // Safer way to read, defaults to empty if missing
                password = providers.gradleProperty("MAPBOX_DOWNLOAD_TOKEN").orNull ?: ""
            }
        }
    }
}

rootProject.name = "AidLink"
include(":app")