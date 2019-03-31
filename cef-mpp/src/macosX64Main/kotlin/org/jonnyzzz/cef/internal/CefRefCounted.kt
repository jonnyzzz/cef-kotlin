package org.jonnyzzz.cef.internal

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.Arena
import kotlinx.cinterop.DeferScope
import kotlinx.cinterop.StableRef
import org.jonnyzzz.cef.generated.KCefBaseRefCounted

internal class KCefRefCountedImpl<T>(
        private val scope: Arena,
        val obj: T
) : KCefBaseRefCounted() {
  private val refsCount = atomic(1)

  override fun addRef() {
    refsCount.incrementAndGet()
  }

  override fun hasAtLeastOneRef(): Int = if (refsCount.value > 0) 1 else 0

  override fun hasOneRef(): Int =  if(refsCount.value == 1) 1 else 0
  override fun release(): Int {
    val newValue = refsCount.decrementAndGet()
    if (newValue >= 1) return 0

    scope.clear()
    return 1
  }
}

internal inline fun <reified T : Any> DeferScope.stablePtr(obj: T): StableRef<T> {
  val ptr = StableRef.create(obj)
  defer { ptr.dispose() }
  return ptr
}

