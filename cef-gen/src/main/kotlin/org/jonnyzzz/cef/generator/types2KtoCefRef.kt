package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun generateWrapKtoCef(mapper: CefTypeMapperGenerator, info: KNRefCountedTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {

    returns(rawStruct.asCPointer())
    addParameter(ParameterSpec.builder("kObj", kInterfaceTypeName).build())

    addStatement("val scope = %T()", arenaType)
    addStatement("val stableRef = scope.%M(%T(scope, kObj))", fnCefStablePrt, cefBaseRefCountedKImpl)
    addCode(generateCValueWithInitBlock(mapper, info).build())

    addStatement("return cValue.%M<%T>().ptr", fnReinterpret, rawStruct)
  }
}

private fun generateTHISUnwrap(into: KNRefCountedTypeInfo, p: KNRefCountedFunction): CodeBlock.Builder = into.run {
  CodeBlock.builder().apply {
    addStatement("initRuntimeIfNeeded()")
    add("val pThis = ${p.THIS.cParamName}")
    add("?.%M<%T>()", fnReinterpret, kStructTypeName)
    add("?.%M?.stablePtr?.value", fnPointed)
    add("?.%M<%T>()?.get()", fnAsStableRef, cefBaseRefCountedKImpl.parameterizedBy(kInterfaceTypeName))
    add("?: error(%S)", "THIS == null for ${rawStruct.simpleName}#${p.cFieldName}")
    addStatement("")
  }
}

private val KNRefCountedPublicFunctionParam.tmpParamName
  get() = kParamName + "K"


private fun generateCValueWithInitBlock(mapper: CefTypeMapperGenerator, info: KNRefCountedTypeInfo): CodeBlock.Builder = info.run {
  CodeBlock.builder().apply {
    beginControlFlow("val cValue = scope.%M<%T>", fnAlloc, kStructTypeName)

    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, kStructTypeName, fnConvert)
    addStatement("cef.base.size = %T.size.%M()", kStructTypeName, fnConvert)
    addStatement("stablePtr.%M = stableRef.asCPointer()", fnValue)


    for (p in info.methods) {
      beginControlFlow("cef.${p.cFieldName} = %M", fnStaticCFunction)
      addStatement((listOf(p.THIS) + p.parameters).joinToString(", ") { it.cParamName } + " ->")
      add(generateTHISUnwrap(info, p).build())

      beginControlFlow("%M", fnMemScoped)

      for (param in p.parameters) {
        add(mapper.mapTypeFromCefToKCode(param, param.kParamName, param.tmpParamName))
      }

      //TODO: include type mapping and conversion here!
      addStatement("val kResult = pThis.obj.${p.kFieldName}(${p.parameters.joinToString(", ") { it.tmpParamName }})")
      add(mapper.mapTypeFromKToCefCode(p, "kResult", "cResult"))

      addStatement("return@staticCFunction cResult")
      endControlFlow()
      endControlFlow()
    }

    addStatement("//CEF base implementation")
    for (p in info.refCountMethods) {
      beginControlFlow("cef.base.${p.cFieldName} = %M", fnStaticCFunction)

      addStatement((listOf(p.THIS) + p.parameters).joinToString(", ") { it.cParamName } + " ->")
      add(generateTHISUnwrap(info, p).build())

      require(p.parameters.isEmpty())
      //also require return type is simple type that needs no mapping

      //TODO: include type mapping and conversion here!
      addStatement("pThis.${p.kFieldName}()")
      endControlFlow()
    }

    endControlFlow()
  }
}

