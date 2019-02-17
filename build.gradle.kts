
plugins {
  id("de.undercouch.download") version "3.4.3" apply false
  id("org.jetbrains.kotlin.multiplatform") version "1.3.21" apply false
  id("org.jetbrains.kotlin.jvm") version "1.3.21" apply false
}


tasks.withType<Wrapper> {
  distributionType = Wrapper.DistributionType.ALL
}

