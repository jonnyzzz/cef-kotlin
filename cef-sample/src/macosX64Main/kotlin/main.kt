package org.jonnyzzz.cef.example

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import org.jonnyzzz.cef.copyFrom
import org.jonnyzzz.cef.generated.KCefAppImplBase
import org.jonnyzzz.cef.generated.KCefBrowserProcessHandlerImplBase
import org.jonnyzzz.cef.generated.KCefBrowserSettingsImplBase
import org.jonnyzzz.cef.generated.KCefClientImplBase
import org.jonnyzzz.cef.generated.KCefSettingsImplBase
import org.jonnyzzz.cef.generated.KCefWindowInfoImplBase
import org.jonnyzzz.cef.interop._cef_browser_process_handler_t
import org.jonnyzzz.cef.interop._cef_browser_t
import org.jonnyzzz.cef.interop._cef_command_line_t
import org.jonnyzzz.cef.interop._cef_list_value_t
import org.jonnyzzz.cef.interop._cef_print_handler_t
import org.jonnyzzz.cef.interop._cef_process_message_t
import org.jonnyzzz.cef.interop._cef_render_process_handler_t
import org.jonnyzzz.cef.interop._cef_resource_bundle_handler_t
import org.jonnyzzz.cef.interop._cef_scheme_registrar_t
import org.jonnyzzz.cef.interop.cef_browser_host_create_browser
import org.jonnyzzz.cef.interop.cef_execute_process
import org.jonnyzzz.cef.interop.cef_initialize
import org.jonnyzzz.cef.interop.cef_main_args_t
import org.jonnyzzz.cef.interop.cef_process_id_t
import org.jonnyzzz.cef.interop.cef_run_message_loop
import org.jonnyzzz.cef.interop.cef_shutdown
import org.jonnyzzz.cef.interop.cef_string_t
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

  val mainArgs = alloc<cef_main_args_t> {
    this.argc = args.size
    this.argv = allocArrayOf((listOf("executable.kexe") + args).map { it.cstr.ptr })
  }

  val browserProcessHandler = object : KCefBrowserProcessHandlerImplBase(this) {
    override fun getPrintHandler(): CPointer<_cef_print_handler_t>? {
      return null
    }

    override fun onBeforeChildProcessLaunch(command_line: CPointer<_cef_command_line_t>?) { }

    override fun onContextInitialized() {
      println("CEF: onContextInitialized")

      val windowInfo = object: KCefWindowInfoImplBase(this@memScoped) {
        init {
          x = 100
          y = 100
          width = 500
          height = 500
          windowName = "cef-kotlin"
        }
      }

      val client = object : KCefClientImplBase(this@memScoped) {
        override fun onProcessMessageReceived(browser: CPointer<_cef_browser_t>?, source_process: cef_process_id_t, message: CPointer<_cef_process_message_t>?): Int {
          return 0
        }
      }

      val url = alloc<cef_string_t> {
        memset(ptr, 0, sizeOf<cef_string_t>().convert())
      }

      val settings = object : KCefBrowserSettingsImplBase(this@memScoped) {

      }

      url.copyFrom("https://jonnyzzz.com")
      cef_browser_host_create_browser(windowInfo.ptr, client.ptr, url.ptr, settings.ptr, null)
    }

    override fun onRenderProcessThreadCreated(extra_info: CPointer<_cef_list_value_t>?) { }
    override fun onScheduleMessagePumpWork(delay_ms: Long) { }
  }

  val app = object : KCefAppImplBase(this) {
    override fun getBrowserProcessHandler(): CPointer<_cef_browser_process_handler_t>? {
      return browserProcessHandler.ptr
    }
    override fun getRenderProcessHandler(): CPointer<_cef_render_process_handler_t>? {
      return null
    }

    override fun getResourceBundleHandler() : CPointer<_cef_resource_bundle_handler_t>? { return null }
    override fun onBeforeCommandLineProcessing(process_type: String?, command_line: CPointer<_cef_command_line_t>?) { }
    override fun onRegisterCustomSchemes(registrar: CPointer<_cef_scheme_registrar_t>?) { }
  }

  val childProcess = cef_execute_process(mainArgs.ptr, null, null)
  if (childProcess >= 0) {
    exitProcess(childProcess)
  }

  val cefSettings = object: KCefSettingsImplBase(this){}.apply {
    browserSubprocessPath = bundlePath
  }

  cef_initialize(mainArgs.ptr, cefSettings.ptr, app.ptr, null)
  println("cef_initialize - complete")

  cef_run_message_loop()

  cef_shutdown()
  println("cef_shutdown - complete")
}

