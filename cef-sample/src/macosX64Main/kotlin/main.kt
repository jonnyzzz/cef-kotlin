package org.jonnyzzz.cef.example

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.DeferScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import org.jonnyzzz.cef.generated.KCefAppImplBase
import org.jonnyzzz.cef.generated.KCefBrowserProcessHandlerImplBase
import org.jonnyzzz.cef.generated.KCefSettingsImplBase
import org.jonnyzzz.cef.interop.*
import kotlin.native.concurrent.freeze
import kotlin.system.exitProcess


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
      return browserProcessHandler.run { ptr }
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
    browserSubprocessPath = "/Users/jonnyzzz/Work/cef-kotlin/cef-sample/build/bin/macosX64/debugExecutable.apps/cef-sample.app/Contents/MacOS/cef-sample.kexe"
  }

  cef_initialize(mainArgs.ptr, cefSettings.run { ptr } , app.run { ptr } , null)
  println("cef_initialize - complete")

  cef_shutdown()
  println("cef_shutdown - complete")

}

