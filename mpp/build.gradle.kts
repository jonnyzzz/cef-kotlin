import org.jonnyzzz.cef.gradle.div
import java.io.FileFilter

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

repositories {
  mavenCentral()
}

val cefHomeMac = project(":deps-cef").run {
  //TODO: use dependency from `:deps-cef`, and publish artifact from it instead.
  file(buildDir / "cef_binaries_base" / "cef_mac", PathValidation.DIRECTORY)
}

val cefDefFile by lazy { buildDir / "cef.def" }

val generateCefDefMac by tasks.creating {
  group = "interop"
  outputs.file(cefDefFile)
  inputs.dir(cefHomeMac)

  doLast {
    val cefHeaders = (cefHomeMac / "include" / "capi").listFiles(FileFilter {
      it.isFile && it.name.startsWith("cef_") && it.name.endsWith(".h")
    })?.sorted()?.joinToString(" \\\n") { "          " + it.relativeTo(cefHomeMac) }

    val defFileText = "" +
            "## generated from task $name from $buildFile\n" +
            "headers = \\\n$cefHeaders\n" +
            "\n\n"


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
}
