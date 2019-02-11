@file:Suppress("PropertyName")

import de.undercouch.gradle.tasks.download.Download

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

cef_builds.filter{ it.key.isCurrent }.forEach { (os2, url) ->

  val os = os2.name.toLowerCase()
  val cefArchiveName = url.split("/").last()
  val cefDest = File(cef_binaries_base, cefArchiveName)
  val cefUnpackDir = File(cef_binaries_base, "cef_$os")

  //TODO: download only one platform
  val download = tasks.create<Download>(::cefDownload.name + "_$os") {
    src(url)
    dest(cefDest)

    overwrite(false)
  }

  val unpack = tasks.create<Sync>(::cefUnpack.name + "_$os") {
    dependsOn(download)
    from({ tarTree(resources.bzip2(cefDest)) })
    into(cefUnpackDir)
    includeEmptyDirs = false
    eachFile {
      path = path.split("/", limit = 2)[1]
    }
  }

  cefDownload.dependsOn(download)
  cefUnpack.dependsOn(unpack)
}


/*
final String os = System.getProperty("os.name", "unknown").toLowerCase()
final boolean isWindows = os.contains("windows")
final boolean isMac = os.contains("mac") || os.contains("os x")
final boolean isLinux = os.contains("linux") || os.contains("unix")
final def windowsLinuxMac = { windows, linux, mac ->
  if (isWindows) return windows
  if (isLinux) return linux
  if (isMac) return mac
  throw new Error("Failed to find suitable OS: $os")
}
ext.windowsLinuxMac = windowsLinuxMac
ext.isWindows = isWindows
ext.isLinux = isLinux
ext.isMac = isMac
ext.os = os.replaceAll("\\s+", "_")
ext.isTeamCityBuild = System.getenv("TEAMCITY_VERSION") != null
*/

enum class OS {
  Windows,
  Linux,
  Mac,
  ;

  val isCurrent get() = current === this

  companion object {
    val current = run {
      val os = System.getProperty("os.name", "unknown").toLowerCase()
      when {
        os.contains("windows") -> Windows
        os.contains("mac") || os.contains("os x") -> Mac
        os.contains("linux") || os.contains("unix") -> Linux
        else -> error("Unknown OS=$os")
      }
    }
  }
}
