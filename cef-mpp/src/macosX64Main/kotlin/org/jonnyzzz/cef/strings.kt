package org.jonnyzzz.cef

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import org.jonnyzzz.cef.generated.cefStringFromUtf8
import org.jonnyzzz.cef.generated.cefStringToUtf8
import org.jonnyzzz.cef.interop.cef_string_t
import org.jonnyzzz.cef.interop.cef_string_utf8_clear
import org.jonnyzzz.cef.interop.cef_string_utf8_t
import platform.posix.memset

fun cef_string_t.copyFrom(str: String) {
  val that = this
  memScoped {
    cefStringFromUtf8(str.cstr.ptr, length.convert(), that.ptr)
  }
}

fun cef_string_t.asString() : String {
  val that = this

  return memScoped {
    val buf = alloc<cef_string_utf8_t>()
    memset(buf.ptr, 0, cef_string_utf8_t.size.convert())
    defer { cef_string_utf8_clear(buf.ptr) }

    cefStringToUtf8(that.str, that.length, buf.ptr)

    ByteArray(buf.length.toInt()) { buf.str!!.get(it) }.stringFromUtf8()
  }
}

fun CPointer<cef_string_t>?.asString() = this?.pointed?.asString()

var cef_string_t.value : String
  get() = asString()
  set(value) { copyFrom(value) }


