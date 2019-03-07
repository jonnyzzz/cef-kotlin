package org.jonnyzzz.cef.generator.c

import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0


fun <T : Any> Iterator<T>.nextOrNull() = when (hasNext()) {
  true -> next()
  else -> null
}

operator fun <R> KProperty0<R>.getValue(This: Any?, property: KProperty<*>): R = this()


interface PushBackIterator<T> : Iterator<T> {
  fun pushBack(t: T)
}

fun <T> Iterator<T>.asPushBack() : PushBackIterator<T> = toPushBackIterator(this)

private fun <T> toPushBackIterator(base: Iterator<T>) = object: PushBackIterator<T> {
  private val pushBackList = LinkedList<T>()

  override fun pushBack(t: T) {
    pushBackList += t
  }

  override fun hasNext() = pushBackList.isNotEmpty() || base.hasNext()

  override fun next(): T {
    if (pushBackList.isNotEmpty()) {
      return pushBackList.removeLast()
    }
    return base.next()
  }
}

