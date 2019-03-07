package org.jonnyzzz.cef.generator.c


fun <T : Any> Iterator<T>.nextOrNull() = when (hasNext()) {
  true -> next()
  else -> null
}
