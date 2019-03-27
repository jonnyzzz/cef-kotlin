package org.jonnyzzz.cef

import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import org.jonnyzzz.cef.interop.cef_string_t
import platform.posix.memset
import kotlin.test.Test


class StringsTest {

  @Test
  fun testString() = memScoped {
    val str = cValue<cef_string_t>()
    memset(str.ptr, 0, cef_string_t.size.convert())

    str.useContents {
      copyFrom("123")
    }

    val actual = str.useContents {
      asString()
    }

    println("actual: $actual")
  }
}

