package org.jonnyzzz.cef.generator.c

fun filterMacros(lines: Iterator<Line>) = sequence {
  while(true) {
    val line = lines.nextOrNull() ?: break

    if (line.text.trim().startsWith("#include")) continue
    if (line.text.trim().startsWith("#ifndef")) continue
    if (line.text.trim().startsWith("#endif")) continue
    if (line.text.trim().startsWith("#define")) continue
    if (line.text.trim().startsWith("#pragma")) continue

    if (line.text.trim().startsWith("#else")) continue
    if (line.text.trim().startsWith("#elsif")) continue

    if (line.text.trim().startsWith("#ifdef")) {
      while (true) {
        @Suppress("NAME_SHADOWING")
        val line = lines.nextOrNull() ?: break
        if (line.text.trim().startsWith("#endif")) break
      }
      continue
    }

    yield(line)
  }
}

