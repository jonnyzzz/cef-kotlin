package org.jonnyzzz.cef

import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.ptr
import org.jonnyzzz.cef.interop.cef_main_args_t


fun MemScope.cefMainArgs(args: Array<String>): cef_main_args_t = alloc {
  this.argc = args.size
  this.argv = allocArrayOf((listOf("executable.kexe") + args).map { it.cstr.ptr })
}

fun cef_main_args_t.wrapKtoCef() = this.ptr

