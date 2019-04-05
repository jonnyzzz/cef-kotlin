package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import org.jonnyzzz.cef.generator.model.CefKNTypeInfo
import org.jonnyzzz.cef.generator.model.DetectedFunctionParam
import org.jonnyzzz.cef.generator.model.KFunctionalField


fun toKNRefCountedTypeInfo(api: KNApiTypeInfo, info: CefKNTypeInfo): KNRefCountedTypeInfo {
  val cefBaseTypeInfo = info.cefBaseTypeInfo

  require(cefBaseTypeInfo != null) { "only CEF Ref counted types, but was $info" }
  require(info.fieldProperties.isEmpty()) { "type $info must not have kFields" }
  require(cefBaseTypeInfo.fieldProperties.isEmpty()) { "type $info must not have kFields in base" }

  return object : KNRefCountedTypeInfo {
    override val api = api
    override val rawStruct = info.rawStruct
    override val kStructTypeName = info.kStructTypeName
    override val kWrapperTypeName = info.kWrapperTypeName
    override val methods = func(info.functionProperties).map { method ->
      object : KNRefCountedPublicFunction, KNRefCountedFunction by method {
        //TODO: O(n^2)
        override val api = api.kMethods.single { it.kFieldName == method.kFieldName }

        override val parameters = run {
          require(method.parameters.map { it.cParamName }.toSet() == this.api.parameters.map { it.kParamName }.toSet() )

          method.parameters.map { param ->
            object: KNRefCountedPublicFunctionParam, KNRefCountedFunctionParam by param {
              //TODO: O(n^2) + kParamName === cParamName
              override val api = this@run.api.parameters.single { it.kParamName == param.cParamName }
            }
          }
        }
      }
    }

    override val refCountMethods = func(cefBaseTypeInfo.functionProperties)
  }
}

private fun param(p: DetectedFunctionParam) = object : KNRefCountedFunctionParam {
  override val cParamName = p.paramName
  override val cParamType = p.paramType
}

private fun func(info: List<KFunctionalField>) = info.map { f ->
  object : KNRefCountedFunction {
    override val cReturnType = f.returnType
    override val cFieldName = f.cFieldName
    override val kFieldName = f.funName

    override val THIS = param(f.THIS)
    override val parameters = f.parameters.map(::param)
  }
}
