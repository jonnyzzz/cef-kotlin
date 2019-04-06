package org.jonnyzzz.cef.generator

import org.jonnyzzz.cef.generator.model.CefKNTypeInfo

fun toKNSimpleTypeInfo(api: KNApiTypeInfo, info: CefKNTypeInfo): KNSimpleTypeInfo {
  require(info.cefBaseTypeInfo == null) { "only simple types are allowed, by was $info" }
  require(info.functionProperties.isEmpty()) { "type $info must not have function properties" }

  return object : KNSimpleTypeInfo {
    override val api = api
    override val rawStruct = info.rawStruct
    override val fields = info.fieldProperties.map { f ->
      val knApiField = api.kFields.single { it.kFieldName == f.propName}
      object : KNSimplePublicField {
        override val api = knApiField
        override val cFieldName = f.cFieldName
        override val kFieldName = f.propName
        override val cReturnType = f.propType
      }
    }
  }
}
