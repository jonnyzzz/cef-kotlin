
plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(kotlin("gradle-plugin", version = "1.3.21"))
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

