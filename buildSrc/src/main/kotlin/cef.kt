package org.jonnyzzz.cef.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File
import kotlin.reflect.full.memberProperties

interface CefConfigurationsBase {
  val cef_include: Configuration
  val cef_debug: Configuration
  val cef_release: Configuration

  val includeDir: File
}

interface CefConfigurations : CefConfigurationsBase {

  fun Executable.linkCefFramework()
}

abstract class CefConfigurationsImpl(project: Project) : CefConfigurationsBase {
  val macOSFrameworkName get() = "ChromiumEmbeddedFramework"

  val os by lazy { OS.current }
  val osName by lazy { os.name.toLowerCase() }

  abstract val cefProject: Project

  override val cef_include by project.configurations.creating
  override val cef_debug by project.configurations.creating
  override val cef_release by project.configurations.creating

  val cef_binaries_base by lazy { File(cefProject.buildDir, "cef_binaries_base") }

  val cefUnpackDir get() = File(cef_binaries_base, "cef_$osName")
  val cefBinariesDir get() = File(cef_binaries_base, "cef_${osName}_binaries")

  override val includeDir by lazy { cefUnpackDir }

  val releaseDir by lazy { cefUnpackDir / "Release" }
  val debugDir by lazy { cefUnpackDir / "Debug" }

  val releaseDirHack by lazy { cefBinariesDir / "Release" }
  val debugDirHack by lazy { cefBinariesDir / "Debug" }
}

fun Project.setupCefConfigurations(action: CefConfigurations.() -> Unit) {
  object : CefConfigurationsImpl(this), CefConfigurations {
    override val cefProject by lazy { project(":deps-cef") }

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
      val bundleDir = File(outputDirectory.path  + ".app")

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

        from(cefBinariesDir){
          into("Contents/Frameworks")
        }

        from(outputDirectory) {
          into("Contents/MacOS")
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
        commandLine(bundleDir / "Contents" / "MacOS" / outputFile.name )
      }
    }
  }.also { config ->
    CefConfigurations::class.memberProperties.forEach {
      if (it.returnType == Configuration::class) {

        val configuration = it.get(config) as Configuration
        dependencies {
          configuration(project(":deps-cef", configuration = configuration.name))
        }
      }
    }
  }.action()
}

fun Project.setupCefConfigurationsProject(action: CefConfigurationsImpl.() -> Unit) {
  if (path != ":deps-cef") error("Only for deps-cef project!")
  object : CefConfigurationsImpl(this) {
    override val cefProject get() = this@setupCefConfigurationsProject
  }.action()
}
