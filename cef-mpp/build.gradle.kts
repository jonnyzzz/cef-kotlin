import java.util.Properties
import org.jonnyzzz.cef.gradle.*
import java.io.FileFilter

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

repositories {
  mavenCentral()
}

val cefDefFile by lazy { buildDir / "cef.def" }

val generateCefDefMac by tasks.creating {
  group = "interop"

  inputs.file(buildFile)
  inputs.file(project(":deps-cef").buildFile)
  outputs.file(cefDefFile)

  doLast {
    val cefHeaders = (cefHomeMac / "include" / "capi").listFiles(FileFilter {
      it.isFile && it.name.startsWith("cef_") && it.name.endsWith(".h")
    })?.sorted()?.joinToString(" ") { it.relativeTo(cefHomeMac).path }

    val def = Properties()

    def.setProperty("headers", cefHeaders)
    // https://youtrack.jetbrains.com/issue/KT-29970
    // def.setProperty("linkerOpts", "-F ${cefHomeMac / "Debug"} -framework Chromium\\ Embedded\\ Framework")

    cefDefFile.printWriter().use { out ->
      def.store(out, "generated from task $name from $buildFile")

      out.println()
      out.println()
    }
  }
}


kotlin {
  macosX64 {
    val main by compilations

    main.cinterops.create("kotlinCefInterop") {
      defFile = cefDefFile
      packageName = "org.jonnyzzz.cef.interop"

      compilerOpts += "-I$cefHomeMac"

      tasks.onEach {
        //TODO: use getter for the task, when available
        if (it.name == interopProcessingTaskName) {
          it.dependsOn(generateCefDefMac)
        }
      }
    }
  }

  val atomicFuVersion = "0.12.1"

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:atomicfu-common:$atomicFuVersion")
      }
    }

    val macosX64Main by getting {
      dependencies {
        implementation("org.jetbrains.kotlinx:atomicfu-native:$atomicFuVersion")
      }
    }
  }
}
