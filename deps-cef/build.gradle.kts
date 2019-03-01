@file:Suppress("PropertyName", "LocalVariableName")

import de.undercouch.gradle.tasks.download.Download
import org.jonnyzzz.cef.gradle.OS
import org.jonnyzzz.cef.gradle.div
import org.jonnyzzz.cef.gradle.setupCefConfigurationsProject
import java.util.*

plugins {
  id("de.undercouch.download")
}

val cefDownload by tasks.creating
val cefUnpack by tasks.creating

setupCefConfigurationsProject {
  val cef_version = "3626.1883.g00e6af4"
  val url = when (os) {
    OS.Linux -> "https://opensource.spotify.com/cefbuilds/cef_binary_3.${cef_version}_linux64.tar.bz2"
    OS.Windows -> "https://opensource.spotify.com/cefbuilds/cef_binary_3.${cef_version}_windows64.tar.bz2"
    OS.Mac -> "https://opensource.spotify.com/cefbuilds/cef_binary_3.${cef_version}_macosx64.tar.bz2"
  }

  val osName = os.name.toLowerCase()
  val cefArchiveName = url.split("/").last()
  val cefDest = File(cef_binaries_base, cefArchiveName)

//TODO: download only one platform
  val download = tasks.create<Download>(::cefDownload.name + "_$osName") {
    cefDownload.dependsOn(this)

    src(url)
    dest(cefDest)

    overwrite(false)
  }

  val unpackTask = tasks.create(::cefUnpack.name + "_$osName") {
    val markerFile = buildDir / "$name.completed"

    cefUnpack.dependsOn(this)
    dependsOn(download)

    //optimization - Sync task works too slow in huge file-tree
    inputs.file(cefDest)
    outputs.file(markerFile)

    doLast {
      delete(markerFile)
      logger.lifecycle("Extracting CEF to $cefUnpackDir...")
      delete(cefUnpackDir)
      copy {
        from({ tarTree(resources.bzip2(cefDest)) })
        into(cefUnpackDir)
        includeEmptyDirs = false
        eachFile {
          path = path.split("/", limit = 2)[1]
        }
      }
      markerFile.writeText("generated ${Date()}")
    }
  }

  artifacts.add(cef_include.name, includeDir) { builtBy(unpackTask) }
  artifacts.add(cef_debug.name, debugDir) { builtBy(unpackTask) }
  artifacts.add(cef_release.name, releaseDir) { builtBy(unpackTask) }

  if (os == OS.Mac) {
    listOf(cef_debug to "Debug" to debugDir,
            cef_release to "Release" to releaseDir).forEach { (tmp, targetDir) ->

      val (configuration, mode) = tmp

      //workaround for https://youtrack.jetbrains.com/issue/KT-29970
      //we have to rename framework to omit spaces

      val originalName = "Chromium Embedded Framework"
      val frameworkDir = cefUnpackDir / mode / "$originalName.framework"
      val targetFrameworkDir = cefBinariesDir / mode.toLowerCase() / "$macOSFrameworkName.framework"

      val prepareTask = tasks.create<Sync>(::cefUnpack.name + "_${osName}_framework_${mode.toLowerCase()}") {
        cefUnpack.dependsOn(this)

        dependsOn(unpackTask)
        from(frameworkDir)
        into(targetFrameworkDir)
        includeEmptyDirs = false
        eachFile {
          name = name.replace(originalName, macOSFrameworkName)
        }
      }

      artifacts.add(configuration.name, targetDir) { builtBy(prepareTask) }
    }
  }
}
