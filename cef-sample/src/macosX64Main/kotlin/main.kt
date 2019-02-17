package org.jonnyzzz.cef.example

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CStructVar
import kotlinx.cinterop.NativePtr
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import org.jonnyzzz.cef.interop._cef_base_ref_counted_t
import org.jonnyzzz.cef.interop.cef_app_t
import org.jonnyzzz.cef.interop.cef_initialize
import org.jonnyzzz.cef.interop.cef_main_args_t
import org.jonnyzzz.cef.interop.cef_settings_t


fun main(args: Array<String>): Unit = memScoped {
  println("CEF Kotlin sample project...")

  val cefSettings = alloc<cef_settings_t> {

  }

  val cefArgs = alloc<cef_main_args_t> {
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

  cef_initialize(cefArgs.ptr, cefSettings.ptr, app.ptr, null)
}

