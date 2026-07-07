// Check if the current build is running inside GitHub Actions
val isCI = System.getenv("GITHUB_ACTIONS") == "true"

pluginManagement {
  repositories {
    if (isCI) {
      // In CI, prioritize official global repositories for reliability
      gradlePluginPortal()
      google()
      mavenCentral()
    } else {
      // Locally, prioritize Myket to avoid regional timeouts, with global fallbacks
      maven { url = uri("https://maven.myket.ir/") }
      gradlePluginPortal()
      google()
      mavenCentral()
    }
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    if (isCI) {
      google()
      mavenCentral()
    } else {
      maven { url = uri("https://maven.myket.ir/") }
      google()
      mavenCentral()
    }
  }
}

rootProject.name = "My Application"

include(":app")