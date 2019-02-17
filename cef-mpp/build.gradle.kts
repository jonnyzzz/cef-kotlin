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
    })?.sorted()?.joinToString(" \\\n") { "          " + it.relativeTo(cefHomeMac) }

    val defFileText = "" +
            "## generated from task $name from $buildFile\n" +
            "headers = \\\n$cefHeaders\n" +
            "\n\n" +
//            "linkerOpts = -F ${cefHomeMac / "Debug"} \\\n" +
//            "             -framework Chromium Embedded Framework\n\n" +
            ""

    println("cef.def:\n$defFileText\n\n")
    cefDefFile.writeText(defFileText)
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
