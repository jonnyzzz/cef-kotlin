apply(from = "settings-setup-plugins.gradle.kts")

rootProject.name = "cef-kotlin"

include("deps-cef")
include("cef-mpp")
include("cef-gen")

include("cef-sample")

enableFeaturePreview("GRADLE_METADATA")

