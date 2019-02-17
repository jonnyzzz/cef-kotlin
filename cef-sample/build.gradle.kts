import org.jonnyzzz.cef.gradle.*

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

repositories {
  mavenCentral()
}


kotlin {
  macosX64 {
    binaries {
      executable {
        compilations["main"].kotlinOptions.freeCompilerArgs = listOf("-Xverbose-phases=linker")
        
        entryPoint = "org.jonnyzzz.cef.example.main"
      }
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib-common"))
        implementation(project(":cef-mpp"))
      }
    }

    val macosX64Main by getting {
      dependencies {
        implementation(project(":cef-mpp"))
      }
    }
  }
}

