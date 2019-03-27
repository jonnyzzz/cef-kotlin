package org.jonnyzzz.cef.example

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.useContents
import org.jonnyzzz.cef.asString
import org.jonnyzzz.cef.copyFrom
import org.jonnyzzz.cef.generated.KCefAppImplBase
import org.jonnyzzz.cef.generated.KCefBrowserProcessHandlerImplBase
import org.jonnyzzz.cef.generated.KCefSettingsImplBase
import org.jonnyzzz.cef.generated.cefStringClear
import org.jonnyzzz.cef.generated.cefStringSet
import org.jonnyzzz.cef.interop.*
import org.jonnyzzz.cef.toDebugString
import platform.posix.memset
import kotlin.native.concurrent.callContinuation1
import kotlin.system.exitProcess
import kotlin.test.Test


fun main(args: Array<String>): Unit = memScoped {
  println("CEF Kotlin sample project...")

  val mainArgs = alloc<cef_main_args_t> {
    this.argc = args.size
    this.argv = allocArrayOf(args.map { it.cstr.ptr })
  }

  val browserProcessHandler = object : KCefBrowserProcessHandlerImplBase(this) {
    override fun getPrintHandler(): CPointer<_cef_print_handler_t>? {
      return null
    }

    override fun onBeforeChildProcessLaunch(command_line: CPointer<_cef_command_line_t>?) { }

    override fun onContextInitialized() {
      println("CEF: onContextInitialized")
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
    browserSubprocessPath = "2323423421234322"
  }

  cef_initialize(mainArgs.ptr, cefSettings.ptr , app.ptr , null)
  println("cef_initialize - complete")

  cef_run_message_loop()

  cef_shutdown()
  println("cef_shutdown - complete")
}

