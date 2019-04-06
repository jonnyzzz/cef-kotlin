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
    addParameter(ParameterSpec.builder("obj", rawStruct.asCPointer()).build())

    addStatement("val pObj = obj.%M", fnPointed)
    for (p in info.fields) {
      addCode(mapper.mapTypeFromCefToKCode(p, "pObj.${p.cFieldName}", p.tmpFieldName))
    }

    addStatement("return %T(\n%L)", kInterfaceTypeName, info.fields.map { p ->
      CodeBlock.of("${p.kFieldName} = ${p.tmpFieldName}")
    }.joinToCode(",\n"))
  }.also { file.addFunction(it.build()) }
}
