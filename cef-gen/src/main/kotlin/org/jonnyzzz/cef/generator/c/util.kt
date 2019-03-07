package org.jonnyzzz.cef.generator.c

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0


fun <T : Any> Iterator<T>.nextOrNull() = when (hasNext()) {
  true -> next()
  else -> null
}

operator fun <R> KProperty0<R>.getValue(This: Any?, property: KProperty<*>): R = this()

