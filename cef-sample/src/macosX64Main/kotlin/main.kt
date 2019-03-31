package org.jonnyzzz.cef.example

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import org.jonnyzzz.cef.cefMainArgs
import org.jonnyzzz.cef.copyFrom
import org.jonnyzzz.cef.generated.KCefApp
import org.jonnyzzz.cef.generated.KCefBrowserProcessHandler
import org.jonnyzzz.cef.generated.KCefBrowserSettings
import org.jonnyzzz.cef.generated.KCefClient
import org.jonnyzzz.cef.generated.KCefSettings
import org.jonnyzzz.cef.generated.KCefWindowInfo
import org.jonnyzzz.cef.generated.wrapKtoCef
import org.jonnyzzz.cef.interop._cef_browser_process_handler_t
import org.jonnyzzz.cef.interop._cef_list_value_t
import org.jonnyzzz.cef.interop.cef_browser_host_create_browser
import org.jonnyzzz.cef.interop.cef_execute_process
import org.jonnyzzz.cef.interop.cef_initialize
import org.jonnyzzz.cef.interop.cef_run_message_loop
import org.jonnyzzz.cef.interop.cef_shutdown
import org.jonnyzzz.cef.interop.cef_string_t
import org.jonnyzzz.cef.wrapKtoCef
import platform.Foundation.NSBundle
import platform.posix.getpid
import platform.posix.memset
import kotlin.system.exitProcess


private fun println(text: String) {
  val pid = getpid().toString().padStart(6)
  kotlin.io.println("$pid: $text")
}

fun main(args: Array<String>): Unit = memScoped {
  println("CEF Kotlin sample project...")

  val bundlePath = NSBundle.mainBundle().executablePath!!
  println("Bundle Path: $bundlePath")
  println("Args: ${args.toList()}")
  val mainArgs = cefMainArgs(args)

  val browserProcessHandler = object : KCefBrowserProcessHandler() {
    override fun onContextInitialized() {
      println("CEF: onContextInitialized")

      val windowInfo = KCefWindowInfo(
              x = 100,
              y = 100,
              width = 500,
              height = 500,
              windowName = "cef-kotlin"
      )

      val client = object : KCefClient() {}

      val url = alloc<cef_string_t> {
        memset(ptr, 0, sizeOf<cef_string_t>().convert())
      }

      val settings = KCefBrowserSettings()

      url.copyFrom("https://jonnyzzz.com")
      cef_browser_host_create_browser(windowInfo.wrapKtoCef(this@memScoped), client.wrapKtoCef(), url.ptr, settings.wrapKtoCef(this@memScoped), null)
    }
  }

  val app = object : KCefApp() {
    override fun getBrowserProcessHandler(): CPointer<_cef_browser_process_handler_t>? {
      return browserProcessHandler.wrapKtoCef()
    }
  }

  val childProcess = cef_execute_process(mainArgs.wrapKtoCef(), null, null)
  if (childProcess >= 0) {
    exitProcess(childProcess)
  }

  val cefSettings = KCefSettings(
          browserSubprocessPath = bundlePath
  )

  cef_initialize(mainArgs.wrapKtoCef(), cefSettings.wrapKtoCef(this@memScoped), app.wrapKtoCef(), null)
  println("cef_initialize - complete")

  cef_run_message_loop()

  cef_shutdown()
  println("cef_shutdown - complete")
}

