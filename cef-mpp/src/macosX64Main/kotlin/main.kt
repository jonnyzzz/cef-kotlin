package org.jonnyzzz.cef

import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import org.jonnyzzz.cef.generated.copyFrom
import org.jonnyzzz.cef.generated.safe_cef_string_from_utf8
import org.jonnyzzz.cef.interop.cef_string_t
import org.jonnyzzz.cef.interop.cef_string_userfree_utf16_alloc
import org.jonnyzzz.cef.interop.cef_string_userfree_utf16_t

val cefKotlinVersion = "0.0.1-SNAPSHOT"

fun String.asCefString(): cef_string_t = memScoped {
  val str: cef_string_userfree_utf16_t = cef_string_userfree_utf16_alloc()!!
  safe_cef_string_from_utf8(cstr.ptr, length.convert(), str)
  return str.pointed
}

fun cef_string_t.copyFrom(str: String) = copyFrom(str.asCefString())

fun cef_string_t.asString() : String = TODO()

var cef_string_t.value : String
  get() = asString()
  set(value) { copyFrom(value) }
