pluginManagement {
  repositories {
    // Check inside the block's compiler scope
    if (System.getenv("GITHUB_ACTIONS") == "true") {
      gradlePluginPortal()
      google()
      mavenCentral()
    } else {
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
    // Check inside the block's compiler scope
    if (System.getenv("GITHUB_ACTIONS") == "true") {
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