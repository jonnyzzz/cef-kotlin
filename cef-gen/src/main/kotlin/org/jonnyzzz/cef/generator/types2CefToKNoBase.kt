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
    addParameter(ParameterSpec.builder("cefStruct", rawStruct.asCPointer()).build())

    addStatement("val pCefStruct = cefStruct.%M", fnPointed)
    generateFunctionBody(info, mapper)
  }.also { file.addFunction(it.build()) }

  FunSpec.builder(wrapCefToKName).apply {
    returns(kInterfaceTypeName)
    addParameter(ParameterSpec.builder("cefStruct", rawStruct.asCValue()).build())

    beginControlFlow("return cefStruct.%M", fnUSeContents)
    addStatement("val pCefStruct = this")
    generateFunctionBody(info, mapper)
    endControlFlow()
  }.also { file.addFunction(it.build()) }
}


private fun FunSpec.Builder.generateFunctionBody(info: KNSimpleTypeInfo,
                                                 mapper: CefTypeMapperGenerator) {
  for (p in info.fields) {
    addCode(mapper.mapTypeFromCefToKCode(p, "pCefStruct.${p.cFieldName}", p.tmpFieldName))
  }

  addStatement("return %T(\n%L)", info.kInterfaceTypeName, info.fields.map { p ->
    CodeBlock.of("${p.kFieldName} = ${p.tmpFieldName}")
  }.joinToCode(",\n"))
}
