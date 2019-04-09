package org.jonnyzzz.cef.generator.model

import org.jonnyzzz.cef.generator.KNRefCountedTypeInfo
import org.jonnyzzz.cef.generator.KNSimpleTypeInfo
import org.jonnyzzz.cef.generator.cefBaseRefCounted
import org.jonnyzzz.cef.generator.toKNRefCountedTypeInfo
import org.jonnyzzz.cef.generator.toKNSimpleTypeInfo

class CefImplModel(val refCounted: List<KNRefCountedTypeInfo>,
                   val simpleTypes: List<KNSimpleTypeInfo>)


fun buildCefImplModel(cefApiModel: CefApiModel): CefImplModel {
  val refCounted = cefApiModel.raw.mapNotNull { (raw, api) ->
    if (raw.rawStruct == cefBaseRefCounted) return@mapNotNull null
    if (raw.cefBaseTypeInfo == null) return@mapNotNull null

    toKNRefCountedTypeInfo(api, raw)
  }

  val simpleTypes = cefApiModel.raw.mapNotNull { (raw, api) ->
    if (raw.rawStruct == cefBaseRefCounted) return@mapNotNull null
    if (raw.cefBaseTypeInfo != null) return@mapNotNull null

    toKNSimpleTypeInfo(api, raw)
  }

  return CefImplModel(refCounted.toList(), simpleTypes.toList())
}
