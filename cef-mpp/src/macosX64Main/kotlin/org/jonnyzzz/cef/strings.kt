package org.jonnyzzz.cef

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import org.jonnyzzz.cef.generated.cefStringFromUtf8
import org.jonnyzzz.cef.interop.cef_string_t

fun cef_string_t.copyFrom(str: String) : Unit = memScoped {
  cefStringFromUtf8(str.cstr.ptr, length.convert(), ptr)
}

fun cef_string_t.asString() : String = "TODO(implement cef_string_t.asString())"

fun CPointer<cef_string_t>?.asString() = this?.pointed?.asString()

var cef_string_t.value : String
  get() = asString()
  set(value) { copyFrom(value) }


