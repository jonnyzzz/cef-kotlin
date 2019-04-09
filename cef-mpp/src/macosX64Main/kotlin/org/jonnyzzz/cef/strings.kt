package org.jonnyzzz.cef

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.useContents
import org.jonnyzzz.cef.generated.cefStringToUtf8
import org.jonnyzzz.cef.interop._cef_string_utf16_t
import org.jonnyzzz.cef.interop.cef_string_t
import org.jonnyzzz.cef.interop.cef_string_userfree_free
import org.jonnyzzz.cef.interop.cef_string_utf8_clear
import org.jonnyzzz.cef.interop.cef_string_utf8_t
import org.jonnyzzz.cef.interop.cef_string_utf8_to_utf16
import org.jonnyzzz.cef.interop.char16Var
import platform.posix.memset
import kotlin.math.min
import kotlin.reflect.KProperty0


fun copyCefString(prop: KProperty0<cef_string_t>, str: _cef_string_utf16_t) {
  prop.get().copyFrom(str)
}

fun MemScope.wrapStringToCefPointerName(str: String) = alloc<cef_string_t> { copyFrom(str)}.ptr
fun wrapStringToCefValueName(str: String) = cValue<cef_string_t> { copyFrom(str)}
fun cef_string_t.wrapStringToCefRaw(str: String) = copyFrom(str)


fun cef_string_t.copyFrom(str: String) {
  cef_string_userfree_free?.invoke(this.ptr)
  //see https://github.com/cztomczak/cefcapi/blob/master/examples/main_win.c
  cef_string_utf8_to_utf16(str, str.length.convert(), this.ptr)
}


fun cef_string_t.copyFrom(str: cef_string_t) {
  //TODO optimize me via a better CEF strings APIs
  copyFrom(str.asString())
}

fun cef_string_t.toDebugString() = buildString {
  append("cef_string_t{")
  append("length=").append(length)
  append(", raw=")
  append(str?.run { readBytes(min(20, length * sizeOf<char16Var>().toInt())).joinToString("") { it.toChar().toString() } })
  append("}")
}

fun cef_string_t.asString() : String {
  val that = this

  return memScoped {
    val buf = alloc<cef_string_utf8_t>()
    memset(buf.ptr, 0, cef_string_utf8_t.size.convert())
    defer { cef_string_utf8_clear(buf.ptr) }

    cefStringToUtf8(that.str, that.length, buf.ptr)

    buf.str!!.readBytes(buf.length.toInt()).stringFromUtf8OrThrow()
  }
}

fun CPointer<cef_string_t>.asString() = this.pointed.asString()
fun CValue<cef_string_t>.asString() = this.useContents { asString() }

var cef_string_t.value : String
  get() = asString()
  set(value) { copyFrom(value) }


