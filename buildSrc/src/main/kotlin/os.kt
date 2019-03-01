package org.jonnyzzz.cef.gradle

enum class OS {
  Windows,
  Linux,
  Mac,
  ;

  val isCurrent get() = current === this

  companion object {
    val current = run {
      val os = System.getProperty("os.name", "unknown").toLowerCase()
      when {
        os.contains("windows") -> Windows
        os.contains("mac") || os.contains("os x") -> Mac
        os.contains("linux") || os.contains("unix") -> Linux
        else -> error("Unknown OS=$os")
      }
    }
  }
}

