package org.jonnyzzz.cef.generator

import org.jonnyzzz.cef.generator.c.parseCFile
import java.io.File
import java.nio.file.Files
import kotlin.streams.toList


fun main() {
  val hackIncludes = File("/Users/jonnyzzz/Work/cef-kotlin/deps-cef/build/cef_binaries_base/cef_mac/include/capi")
  val allHeaders = Files.walk(hackIncludes.toPath())
          .map { it.toFile() }
          .filter { it.isFile && it.name.endsWith(".h")}
          .toList()

  println("Header files to process:\n${allHeaders.joinToString("\n"){"  $it"}}")

  for (header in allHeaders) {
    try {
      parseCFile(header)
    } catch (t: Throwable) {
      throw Error("Failed to parse ${header.relativeTo(hackIncludes)}", t)
    }
  }

}
