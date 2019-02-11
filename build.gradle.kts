
plugins {
  id("de.undercouch.download") version "3.4.3" apply false
}


tasks.withType<Wrapper>() {
  distributionType = Wrapper.DistributionType.ALL
}

