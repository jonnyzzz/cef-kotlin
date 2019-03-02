package org.jonnyzzz.cef.example

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import org.jonnyzzz.cef.generated.CefSettings
import org.jonnyzzz.cef.interop._cef_base_ref_counted_t
import org.jonnyzzz.cef.interop.cef_app_t
import org.jonnyzzz.cef.interop.cef_execute_process
import org.jonnyzzz.cef.interop.cef_initialize
import org.jonnyzzz.cef.interop.cef_main_args_t
import kotlin.system.exitProcess


fun main(args: Array<String>): Unit = memScoped {
  println("CEF Kotlin sample project...")

  val mainArgs = alloc<cef_main_args_t> {
    this.argc = args.size
    this.argv = allocArrayOf(args.map { it.cstr.ptr })
  }

  val app = alloc<cef_app_t> {
    this.base.size = sizeOf<cef_app_t>().convert()

    this.base.add_ref = staticCFunction<CPointer<_cef_base_ref_counted_t>?, Unit> {  }
    this.base.has_at_least_one_ref = staticCFunction<CPointer<_cef_base_ref_counted_t>?, Int> { 0 }
    this.base.has_one_ref = staticCFunction<CPointer<_cef_base_ref_counted_t>?, Int> { 0 }
    this.base.release = staticCFunction<CPointer<_cef_base_ref_counted_t>?, Int> { 0 }
  }

  val childProcess = cef_execute_process(mainArgs.ptr, null, null)
  if (childProcess >= 0) {
    exitProcess(childProcess)
  }

  val cefSettings = CefSettings().apply {
    browser_subprocess_path = "/Users/jonnyzzz/Work/cef-kotlin/cef-sample/build/bin/macosX64/debugExecutable.apps/cef-sample.app/Contents/MacOS/cef-sample.kexe"
  }

  cef_initialize(mainArgs.ptr, cefSettings.run { ptr }  , app.ptr, null)
}

