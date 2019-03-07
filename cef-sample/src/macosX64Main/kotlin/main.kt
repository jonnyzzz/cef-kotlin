package org.jonnyzzz.cef.example

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import org.jonnyzzz.cef.generated.KCefAppImplBase
import org.jonnyzzz.cef.generated.KCefSettingsImplBase
import org.jonnyzzz.cef.interop.*
import kotlin.system.exitProcess


fun main(args: Array<String>): Unit = memScoped {
  println("CEF Kotlin sample project...")

  val mainArgs = alloc<cef_main_args_t> {
    this.argc = args.size
    this.argv = allocArrayOf(args.map { it.cstr.ptr })
  }

  val app = object : KCefAppImplBase(this) {
    override fun getBrowserProcessHandler() = null
    override fun getRenderProcessHandler() = null
    override fun getResourceBundleHandler() : CPointer<_cef_resource_bundle_handler_t>? { return null }
    override fun onBeforeCommandLineProcessing(p0: String?, p1: CPointer<_cef_command_line_t>?) = Unit
    override fun onRegisterCustomSchemes(p0: CPointer<_cef_scheme_registrar_t>?) = Unit
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

