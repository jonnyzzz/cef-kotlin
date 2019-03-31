
plugins {
  id("de.undercouch.download") version "3.4.3" apply false
  id("org.jetbrains.kotlin.multiplatform") version "1.3.30-eap-125" apply false
  id("org.jetbrains.kotlin.jvm") version "1.3.30-eap-125" apply false
  id("org.jonnyzzz.cef-kotlin") version "1.3.30-eap-125" apply false
}


tasks.withType<Wrapper> {
  distributionType = Wrapper.DistributionType.ALL
}

subprojects {

  repositories {
    mavenCentral()
    maven(url="https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url="https://dl.bintray.com/kotlin/kotlinx")
  }

}
