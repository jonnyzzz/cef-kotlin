import org.jonnyzzz.cef.gradle.*

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

setupCefConfigurations {
  kotlin {
    macosX64 {
      compilations["main"].kotlinOptions.freeCompilerArgs = listOf("-Xverbose-phases=linker")

      binaries {
        executable {

          linkerOpts.addAll(listOf(
                  "-F", "${cefBinariesDir}",
                  "-framework", macOSFrameworkName)
          )

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
