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

data class CefSymbolsUrl(val debug: String,
                         val release: String)

fun downloadAndUnpackBZip2(
        url: String,
        downloadTaskName : String,
        unpackTaskName : String,
        downloadBase: File,
        downloadUnpackedDir: File
        ): Pair<Task, Task> {
  val cefDest = downloadBase / (url.split("/").last())

  val download = tasks.create<Download>(downloadTaskName) {
    src(url)
    dest(cefDest)
    overwrite(false)
  }

  val unpackTask = tasks.create(unpackTaskName) {
    val markerFile = buildDir / "$name.completed"
    dependsOn(download)

    //optimization - Sync task works too slow in huge file-tree
    inputs.file(cefDest)
    outputs.file(markerFile)

    doLast {
      delete(markerFile)
      logger.lifecycle("Extracting CEF to $downloadUnpackedDir...")
      delete(downloadUnpackedDir)
      copy {
        from({ tarTree(resources.bzip2(cefDest)) })
        into(downloadUnpackedDir)
        includeEmptyDirs = false
        eachFile {
          path = path.split("/", limit = 2)[1]
        }
      }
      markerFile.writeText("generated ${Date()}")
    }
  }

  return download to unpackTask
}

setupCefConfigurationsProject {
  val cef_version = "3626.1883.g00e6af4"
  val url = when (os) {
    OS.Linux -> "http://opensource.spotify.com/cefbuilds/cef_binary_3.${cef_version}_linux64.tar.bz2"
    OS.Windows -> "http://opensource.spotify.com/cefbuilds/cef_binary_3.${cef_version}_windows64.tar.bz2"
    OS.Mac -> "http://opensource.spotify.com/cefbuilds/cef_binary_3.${cef_version}_macosx64.tar.bz2"
  }

  val cef_symbols = "3.3626.1892.g7cb6de3"
  val symbols = when(os) {
    OS.Mac -> CefSymbolsUrl(
            debug =   "http://opensource.spotify.com/cefbuilds/cef_binary_${cef_symbols}_macosx64_debug_symbols.tar.bz2",
            release = "http://opensource.spotify.com/cefbuilds/cef_binary_${cef_symbols}_macosx64_release_symbols.tar.bz2")
    else -> TODO("Add symbols URLs")
  }
  val osName = os.name.toLowerCase()
  val (download, unpackTask) = downloadAndUnpackBZip2(url,
          ::cefDownload.name + "_$osName",
          ::cefUnpack.name + "_$osName",
          cef_binaries_base,
          cefUnpackDir
  )

  cefDownload.dependsOn(download)
  cefUnpack.dependsOn(unpackTask)

  val(downloadSymbols, unpackSymbols) = downloadAndUnpackBZip2(symbols.debug,
          ::cefDownload.name + "_${osName}_debug_symbols",
          ::cefUnpack.name + "_${osName}_debug_symbols",
          cef_binaries_base,
          cefUnpackDebugSymbolsDir
  )
  cefDownload.dependsOn(downloadSymbols)
  cefUnpack.dependsOn(unpackSymbols)


  artifacts.add(cef_include.name, includeDir) { builtBy(unpackTask) }
  artifacts.add(cef_debug.name, debugDir) { builtBy(unpackTask, unpackSymbols) }
  artifacts.add(cef_debug_symbols.name, cefUnpackDebugSymbolsDir) { builtBy(unpackSymbols) }
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
