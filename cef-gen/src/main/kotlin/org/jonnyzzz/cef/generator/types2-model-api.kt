package org.jonnyzzz.cef.generator

import org.jonnyzzz.cef.generator.model.CefKNTypeInfo
import org.jonnyzzz.cef.generator.model.KDocumented

fun toKNApiTypeInfo(info: CefKNTypeInfo): KNApiTypeInfo = object : KNApiTypeInfo, KDocumented by info {
  override val kInterfaceTypeName = info.kInterfaceTypeName

  override val kMethods = info.functionProperties.map { f ->
    object : KNApiFunction, KDocumented by f {
      override val kFieldName = f.funName
      override val kReturnType = f.returnType
      override val parameters = f.parameters.map { param ->
        object : KNApiFunctionParam {
          override val kParamName = param.paramName
          override val kParamType = param.paramType
        }
      }
    }
  }

  override val kFields = info.fieldProperties.map { p ->
    object : KNApiField, KDocumented by p {
      override val kFieldName = p.propName
      override val kReturnType = p.propType
    }
  }
}
