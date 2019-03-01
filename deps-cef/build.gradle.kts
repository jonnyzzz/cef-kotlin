@file:Suppress("PropertyName")

import de.undercouch.gradle.tasks.download.Download
import org.jonnyzzz.cef.gradle.*
import java.util.*

plugins {
  id("de.undercouch.download")
}

val cefVersion = "3626.1883.g00e6af4"
val cef_builds = mapOf(
        OS.Linux to "http://opensource.spotify.com/cefbuilds/cef_binary_3.${cefVersion}_linux64.tar.bz2",
        OS.Windows to "http://opensource.spotify.com/cefbuilds/cef_binary_3.${cefVersion}_windows64.tar.bz2",
        OS.Mac to "http://opensource.spotify.com/cefbuilds/cef_binary_3.${cefVersion}_macosx64.tar.bz2"
)

val cefDownload by tasks.creating
val cefUnpack by tasks.creating

val cef_binaries_base by extra { File(buildDir, "cef_binaries_base") }

val cef_include by configurations.creating
val cef_debug by configurations.creating
val cef_release by configurations.creating

cef_builds.filter{ it.key.isCurrent }.forEach { (os2, url) ->

  val os = os2.name.toLowerCase()
  val cefArchiveName = url.split("/").last()
  val cefDest = File(cef_binaries_base, cefArchiveName)
  val cefUnpackDir = File(cef_binaries_base, "cef_$os")
  val cefBinariesDir = File(cef_binaries_base, "cef_${os}_binaries")

  //TODO: download only one platform
  val download = tasks.create<Download>(::cefDownload.name + "_$os") {
    cefDownload.dependsOn(this)

    src(url)
    dest(cefDest)

    overwrite(false)
  }

  val unpack = tasks.create(::cefUnpack.name + "_$os") {
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

  listOf(cef_include to "include",
          cef_debug to "Debug",
          cef_release to "Release").forEach { (configuration, mode) ->

    val (dir, task) = when {
      os2 == OS.Mac && configuration !== cef_include -> {
        //workaround for https://youtrack.jetbrains.com/issue/KT-29970
        //we have to rename framework to omit spaces

        val originalName = "Chromium Embedded Framework"
        val newName = "ChromiumEmbeddedFramework"

        val frameworkDir = cefUnpackDir / mode / "$originalName.framework"
        val targetDir = cefBinariesDir / mode.toLowerCase() / "$newName.framework"

        val prepareTask = tasks.create<Sync>(::cefUnpack.name + "_${os}_framework_${mode.toLowerCase()}") {
          cefUnpack.dependsOn(this)

          dependsOn(unpack)
          from(frameworkDir)
          into(targetDir)
          includeEmptyDirs = false
          eachFile {
            this.name = this.name.replace(originalName, newName)
          }
        }

        targetDir to prepareTask
      }

      else -> cefUnpackDir / mode to unpack
    }

    artifacts.add(configuration.name, dir) {
      builtBy(task)
    }
  }
}

