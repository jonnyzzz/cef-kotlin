package org.jonnyzzz.cef.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File

class ClientCefConfigurationsImpl(
        val project: Project
) : CefConfigurationsImpl(project), CefConfigurations {
  override val cefProject by lazy { project.project(":deps-cef") }

  val Executable.cefBinariesDir
    get() = when (buildType) {
      NativeBuildType.DEBUG -> debugDir
      NativeBuildType.RELEASE -> releaseDir
    }

  val Executable.cefBinariesConfiguration
    get() = when (buildType) {
      NativeBuildType.DEBUG -> cef_debug
      NativeBuildType.RELEASE -> cef_release
    }

  val Executable.cefBinariesDirHack
    get() = when {
      os != OS.Mac -> cefBinariesDir
      else -> when (buildType) {
        NativeBuildType.DEBUG -> debugDirHack
        NativeBuildType.RELEASE -> releaseDirHack
      }
    }

  override fun Executable.linkCefFramework() {
    //workaround for https://youtrack.jetbrains.com/issue/KT-29970
    //we have to rename framework to omit spaces
    linkerOpts.addAll(listOf(
            "-F", "$cefBinariesDirHack",
            "-framework", macOSFrameworkName)
    )
    linkTask.dependsOn(cefBinariesConfiguration)

    setupBundle(this@ClientCefConfigurationsImpl, this@linkCefFramework)
  }
}

private fun setupBundle(cef: ClientCefConfigurationsImpl, executable: Executable) = cef.run {
  project.run {
    executable.run {
      val bundleDir = File(outputDirectory.path + ".apps") / "$baseName.app"

      val taskNameSuffix = "${buildType.name.toLowerCase().capitalize()}Bundle_$baseName"
      val infoPlistFile = buildDir / "$taskNameSuffix.plist"

      val generateInfoPlist = project.tasks.create<InfoPlistTask>("generate${taskNameSuffix}plist") {
        targetFile = infoPlistFile

        icons = "AAA"
        bundleIdentifier = "org.jonnyzzz.cef.kotlin.sample"
        bundleName = "CEF-Kotlin-Sample"
        executableName = outputFile.name
      }

      val buildBundleTask = project.tasks.create<Sync>("build$taskNameSuffix") {
        destinationDir = bundleDir

        from(cefBinariesDir) {
          into("Contents/Frameworks")
        }

        from(outputDirectory) {
          into("Contents/MacOS")
        }

        into("Contents/Frameworks/${baseName}Helper.app") {
          from(outputDirectory) {
            into("Contents/MacOS")
          }

          from(infoPlistFile) {
            into("Contents")
            eachFile {
              name = "Info.plist"
            }
          }
        }

        from(infoPlistFile) {
          into("Contents")
          eachFile {
            name = "Info.plist"
          }
        }

        dependsOn(generateInfoPlist)
        dependsOn(cefBinariesConfiguration)
        dependsOn(linkTask)
        //TODO: include info.plist
      }

      project.tasks.create<Exec>("run$taskNameSuffix") {
        dependsOn(buildBundleTask)
        commandLine(bundleDir / "Contents" / "MacOS" / outputFile.name)
      }
    }
  }
}
