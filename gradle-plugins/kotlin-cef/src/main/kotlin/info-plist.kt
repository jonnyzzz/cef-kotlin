package org.jonnyzzz.cef.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class InfoPlistTask : DefaultTask() {
  @Input var bundleIdentifier : String? = null
  @Input var bundleName : String? = null
  @Input var executableName : String? = null
  @Input var icons : String? = null

  @OutputFile
  var targetFile : File? = null

  interface Builder {
    fun property(key: String, value: Boolean)
    fun property(key: String, value: String)
    fun property(key: String, vararg values: Pair<String,String>)

    operator fun String.rem(value: Boolean) = property(this, value)
    operator fun String.rem(value: String) = property(this, value)
    operator fun String.rem(values: List<Pair<String,String>>) = property(this, *values.toTypedArray())
  }

  private fun buildInfo(b : Builder.() -> Unit) = buildString {
    //TODO: use XML builder
    appendln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    appendln("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">")
    appendln("<plist version=\"1.0\">")
    appendln("<dict>")

    object:Builder {
      override fun property(key: String, value: Boolean) {
        appendln("<key>$key</key>")
        if (value) {
          appendln("<true/>")
        } else {
          appendln("<false/>")
        }
      }

      override fun property(key: String, value: String) {
        appendln("<key>$key</key>")
        appendln("<string>$value</string>")
      }

      override fun property(key: String, vararg values: Pair<String,String>) {
        appendln("<key>$key</key>")
        appendln("<dict>")
        for ((k,v) in values) {
          appendln("  <key>$k</key>")
          appendln("  <string>$v</string>")
        }
        appendln("</dict>")
      }
    }.b()


    appendln("</dict>")
    appendln("</plist>")
    appendln()
  }


  @TaskAction
  fun `action!`() {
    val text = buildInfo {
      "CFBundleDevelopmentRegion" % "English"
      "CFBundleExecutable" % executableName!!
      icons?.let {
        "CFBundleIconFile" % it
      }
      "CFBundleIdentifier" % bundleIdentifier!!
      "CFBundleInfoDictionaryVersion" % "6.0"
      "CFBundleName" % bundleName!!
      "CFBundlePackageType" % "APPL"
      "CFBundleSignature" % "????"
      "CFBundleVersion" % "1.0"
      "LSEnvironment" % listOf( "MallocNanoZone" to "0" )
      "LSMinimumSystemVersion" % "10.11.0"
      "NSSupportsAutomaticGraphicsSwitching" % true
      "NSHighResolutionCapable" % true
      "LSFileQuarantineEnabled" % true

      /*
      "LSUIElement" % "1"
      "NSUIElement" % false
      */
    }

    (targetFile?:error("target file is not set")).writeText(text)
  }
}
