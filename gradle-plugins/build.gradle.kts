
plugins {
  kotlin("jvm") version "1.3.21" apply false
  id("com.gradle.plugin-publish") version "0.10.1" apply false
}


tasks.withType<Wrapper> {
  distributionType = Wrapper.DistributionType.ALL
}

subprojects {
  repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
  }
}

allprojects {
  version = "0.0.1"
  group = "org.jonnyzzz.cef.plugins"
}

