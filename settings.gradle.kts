
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven(url="https://dl.bintray.com/kotlin/kotlin-eap")
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("org.jonnyzzz.cef")) {
        useModule("org.jonnyzzz.cef.plugins:kotlin-cef:LOCAL_BUILD")
      }
    }
  }
}

rootProject.name = "cef-kotlin"



include("deps-cef")
include("cef-mpp")
include("cef-gen")

include("cef-sample")

enableFeaturePreview("GRADLE_METADATA")


includeBuild("gradle-plugins") {
  dependencySubstitution {
    substitute(module("org.jonnyzzz.cef.plugins:kotlin-cef")).with(project(":kotlin-cef"))
  }
}
