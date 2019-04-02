package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jonnyzzz.cef.generator.model.wrapKtoCefName


interface KNRefCountedTFunctionParam {
  val paramName: String
}

interface KNRefCountedFunction {
  val cFieldName : String
  val kFieldName : String
  val THIS : KNRefCountedTFunctionParam
  val parameters: List<KNRefCountedTFunctionParam>
}

interface KNRefCountedTypeInfo {
  val rawStruct: ClassName
  val kInterfaceTypeName : ClassName
  val kStructTypeName : ClassName

  val methods : List<KNRefCountedFunction>
  val refCountMethods : List<KNRefCountedFunction>
}

fun generateWrapKtoCef2(info: KNRefCountedTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    returns(rawStruct.asCPointer())
    receiver(kInterfaceTypeName)

    addStatement("return $wrapKtoCefName(this)")
  }
}

fun generateWrapKtoCef(info: KNRefCountedTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {

    returns(rawStruct.asCPointer())
    addParameter(ParameterSpec.builder("obj", kInterfaceTypeName).build())

    addStatement("val scope = %T()", arenaType)
    addStatement("val stableRef = scope.%M(%T(scope, obj))", fnCefStablePrt, cefBaseRefCountedKImpl)
    addCode(generateCValueWithInitBlock(info).build())

    addStatement("return cValue.%M<%T>().ptr", fnReinterpret, rawStruct)
  }
}


private fun generateTHISUnwrap(into: KNRefCountedTypeInfo, p: KNRefCountedFunction): CodeBlock.Builder = into.run {
  CodeBlock.builder().apply {
    addStatement("initRuntimeIfNeeded()")
    add("val pThis = ${p.THIS.paramName}")
    add("?.%M<%T>()", fnReinterpret, kStructTypeName)
    add("?.%M?.stablePtr?.value", fnPointed)
    add("?.%M<%T>()?.get()", fnAsStableRef, cefBaseRefCountedKImpl.parameterizedBy(kInterfaceTypeName))
    add("?: error(%S)", "THIS == null for $rawStruct#${p.cFieldName}")
    addStatement("")
  }
}

private fun generateCValueWithInitBlock(info: KNRefCountedTypeInfo): CodeBlock.Builder = info.run {
  CodeBlock.builder().apply {
    beginControlFlow("val cValue = scope.%M<%T>", fnAlloc, kStructTypeName)

    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, kStructTypeName, fnConvert)
    addStatement("cef.base.size = %T.size.%M()", kStructTypeName, fnConvert)
    addStatement("stablePtr.%M = stableRef.asCPointer()", fnValue)

    (
            info.methods.map { Triple(it, "cef.", "pThis.obj.") } +
     info.refCountMethods.map { Triple(it, "cef.base.", "pThis.") }
            ).forEach { (p, cPrefix, kPrefix) ->
      beginControlFlow("$cPrefix${p.cFieldName} = %M", fnStaticCFunction)

      addStatement((listOf(p.THIS) + p.parameters).joinToString(", ") { it.paramName } + " ->")
      add(generateTHISUnwrap(info, p).build())

      //TODO: include type mapping and conversion here!
      addStatement("$kPrefix${p.kFieldName}(${p.parameters.joinToString(", ") { it.paramName }})")
      endControlFlow()
    }

    endControlFlow()
  }
}

