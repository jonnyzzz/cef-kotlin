package org.jonnyzzz.cef

import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.useContents
import org.jonnyzzz.cef.generated.cefStringClear
import org.jonnyzzz.cef.interop.cef_string_t
import platform.posix.memset
import kotlin.test.Test


class StringsTest {

  @Test
  fun testString() = memScoped {
    val str = alloc<cef_string_t> {
      memset(ptr, 0, sizeOf<cef_string_t>().convert())
      cefStringClear(ptr)
    }

    println(str.toDebugString())
    str.copyFrom("123")
    println(str.toDebugString())

    println("actual: ${str.asString()}")
  }
}

