import java.util.Properties
import org.jonnyzzz.cef.gradle.*
import java.io.FileFilter

plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

setupCefConfigurations {

  val cefDefFile by lazy { buildDir / "cef.def" }

  val generateCefDef by tasks.creating {
    group = "interop"

    dependsOn(cef_include)
    outputs.file(cefDefFile)

    doLast {
      println("CEF Includes: ${includeDir}")
      val cefHeaders = (includeDir / "include" / "capi").listFiles(FileFilter {
        it.isFile && it.name.startsWith("cef_") && it.name.endsWith(".h")
      })?.sorted()?.joinToString(" ") { it.relativeTo(includeDir).path } ?: error("No files!")

      val def = Properties()

      def.setProperty("headers", cefHeaders)

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

        compilerOpts += "-I${includeDir}"

        setupInteropProcessingTask(project) {
          dependsOn(cef_include)
          dependsOn(generateCefDef)
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
        kotlin.srcDir(buildDir / "generated-cef" / "api")
        kotlin.srcDir(buildDir / "generated-cef" / "k2cef")
        kotlin.srcDir(buildDir / "generated-cef" / "misc")

        dependencies {
          implementation("org.jetbrains.kotlinx:atomicfu-native:$atomicFuVersion")
        }
      }
    }
  }

}
