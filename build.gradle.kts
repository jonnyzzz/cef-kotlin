
plugins {
  id("de.undercouch.download") version "3.4.3" apply false
  id("org.jetbrains.kotlin.multiplatform") apply false
  id("org.jetbrains.kotlin.jvm") apply false
}


tasks.withType<Wrapper> {
  distributionType = Wrapper.DistributionType.ALL
}

subprojects {

  repositories {
    mavenCentral()
  }

}
