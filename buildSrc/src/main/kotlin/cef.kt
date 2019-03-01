package org.jonnyzzz.cef.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.io.File
import kotlin.reflect.full.memberProperties

interface CefConfigurations {
  val macOSFrameworkName get() = "ChromiumEmbeddedFramework"

  val cef_include: Configuration
  val cef_debug: Configuration
  val cef_release: Configuration

  val includeDir: File
  val releaseDir: File
  val debugDir: File

  val Executable.cefBinariesDir
    get() = when (buildType) {
      NativeBuildType.DEBUG -> debugDir
      NativeBuildType.RELEASE -> releaseDir
    }

}

abstract class CefConfigurationsImpl(project: Project) : CefConfigurations {
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
  override val releaseDir by lazy { (if (os == OS.Mac) cefBinariesDir else cefUnpackDir) / "Release" }
  override val debugDir by lazy { (if (os == OS.Mac) cefBinariesDir else cefUnpackDir) / "Debug" }
}

fun Project.setupCefConfigurations(action: CefConfigurations.() -> Unit) {
  object : CefConfigurationsImpl(this) {
    override val cefProject by lazy { project(":deps-cef") }
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
