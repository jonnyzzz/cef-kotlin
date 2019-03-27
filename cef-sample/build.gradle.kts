import org.jonnyzzz.cef.gradle.*

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

setupCefConfigurations {
  kotlin {
    
    //TODO: setup TWO binaries - one for renderer processes, the other for entry-point
    macosX64 {
      compilations["main"].kotlinOptions.freeCompilerArgs = listOf("-Xverbose-phases=linker")

      binaries {
        executable {

          linkCefFramework()

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
}
