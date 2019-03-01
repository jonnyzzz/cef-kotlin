package org.jonnyzzz.cef.example

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import org.jonnyzzz.cef.generated.copyFrom
import org.jonnyzzz.cef.interop._cef_base_ref_counted_t
import org.jonnyzzz.cef.interop.cef_app_t
import org.jonnyzzz.cef.interop.cef_execute_process
import org.jonnyzzz.cef.interop.cef_initialize
import org.jonnyzzz.cef.interop.cef_main_args_t
import org.jonnyzzz.cef.interop.cef_settings_t
import org.jonnyzzz.cef.interop.cef_string_from_utf8
import org.jonnyzzz.cef.interop.cef_string_userfree_utf16_alloc
import org.jonnyzzz.cef.interop.cef_string_userfree_utf16_t
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

  val cefSettings = alloc<cef_settings_t> {
    val str: cef_string_userfree_utf16_t? = cef_string_userfree_utf16_alloc()!!
    val path = "/Users/jonnyzzz/Work/cef-kotlin/cef-sample/build/bin/macosX64/debugExecutable.apps/cef-sample.app/Contents/MacOS/cef-sample.kexe"
    (cef_string_from_utf8!!)(path.cstr.ptr, path.length.convert(), str)
    browser_subprocess_path.copyFrom(str!!.pointed)
  }

  cef_initialize(mainArgs.ptr, cefSettings.ptr, app.ptr, null)
}

