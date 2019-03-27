package org.jonnyzzz.cef.internal

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.DeferScope
import kotlinx.cinterop.StableRef
import org.jonnyzzz.cef.generated.KCefBaseRefCounted

internal class KCefRefCountedImpl : KCefBaseRefCounted {
  private val refsCount = atomic(1)

  override fun addRef() {
    refsCount.incrementAndGet()
  }

  override fun hasAtLeastOneRef() = if (refsCount.value > 0) 1 else 0

  override fun hasOneRef() =  if(refsCount.value == 1) 1 else 0
  override fun release() = if (refsCount.decrementAndGet() >= 1) 1 else 0
}

internal inline fun <reified T : Any> DeferScope.stablePtr(obj: T): StableRef<T> {
  val ptr = StableRef.create(obj)
  defer { ptr.dispose() }
  return ptr
}

