package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.joinToCode


private val KNSimplePublicField.tmpFieldName
  get() = kFieldName + "K"

fun generateWrapKtoCefNoBase(file : FileSpec.Builder, info: KNSimpleTypeInfo, mapper: CefTypeMapperGenerator): Unit = info.run {

  FunSpec.builder(wrapCefToKName).apply {
    returns(kInterfaceTypeName)
    addParameter(ParameterSpec.builder("pCefStruct", rawStruct).build())

    for (p in info.fields) {
      addCode(mapper.mapTypeFromCefToKCode(p, "pCefStruct.${p.cFieldName}", p.tmpFieldName))
    }

    addStatement("return %T(\n%L)", info.kInterfaceTypeName, info.fields.map { p ->
      CodeBlock.of("${p.kFieldName} = ${p.tmpFieldName}")
    }.joinToCode(",\n"))

  }.also { file.addFunction(it.build()) }

  FunSpec.builder(wrapCefToKName).apply {
    returns(kInterfaceTypeName)
    addParameter(ParameterSpec.builder("cefStruct", rawStruct.asCPointer()).build())

    addStatement("return $wrapCefToKName(cefStruct.%M)", fnPointed)
  }.also { file.addFunction(it.build()) }

  FunSpec.builder(wrapCefToKName).apply {
    returns(kInterfaceTypeName)
    addParameter(ParameterSpec.builder("cefStruct", rawStruct.asCValue()).build())

    beginControlFlow("return cefStruct.%M", fnUSeContents)
    addStatement("return $wrapCefToKName(this)")
    endControlFlow()
  }.also { file.addFunction(it.build()) }
}

