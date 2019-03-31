package org.jonnyzzz.cef.generator.kn

import java.lang.StringBuilder


fun String.tokenizeNames() = sequence<String> {
  val s = StringBuilder()

  suspend fun SequenceScope<String>.yield() {
    val t = s.toString()
    s.setLength(0)

    when {
      t == "byindex" -> {
        yield("by")
        yield("index")
      }
      t.isNotEmpty() -> yield(t)
    }
  }

  for (ch in toCharArray()) {
    when {
      ch == '_' -> yield()
      ch.isDigit() -> {
        yield()
        s.append(ch)
        yield()
      }
      else -> s.append(ch)
    }
  }

  yield()
}

