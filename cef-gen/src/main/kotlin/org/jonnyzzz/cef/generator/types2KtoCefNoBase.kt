package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.UNIT


private val KNSimplePublicField.tmpFieldName
  get() = kFieldName + "C"

fun generateWrapCefToKNoBase(file : FileSpec.Builder, info: KNSimpleTypeInfo, mapper: CefTypeMapperGenerator): Unit = info.run {
  FunSpec.builder(wrapKtoCefPointerName).apply {
    addModifiers(KModifier.PRIVATE)
    returns(UNIT)
    receiver(rawStruct)
    addParameter(ParameterSpec.builder("cefStruct", kInterfaceTypeName).build())

    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, rawStruct, fnConvert)

    //cases
    // - primitive type => assign
    // - CefRefCounted => class wrap
    // - _cef_string_^ => copyFrom()

    for (p in info.fields) {
      addCode(mapper.mapTypeFromKToCefCode(p, "cefStruct.${p.kFieldName}", p.tmpFieldName))
    }

    for (p in info.fields) {
      when (p.cReturnType) {
        cefString16,
        cefString16.asNullableCPointer() ->
          addStatement("%M(this::${p.cFieldName}, ${p.tmpFieldName})", MemberName("org.jonnyzzz.cef", "copyCefString"))

        else ->
          addStatement("${p.cFieldName} = ${p.tmpFieldName}")
      }
    }
  }.build().also { file.addFunction(it) }

  FunSpec.builder(wrapKtoCefPointerName).apply {
    returns(rawStruct.asCPointer())
    receiver(memberScopeType)
    addParameter(ParameterSpec.builder("cefStruct", kInterfaceTypeName).build())

    addStatement("return %M<%T>{ $wrapKtoCefPointerName(cefStruct) }.ptr", fnAlloc, rawStruct)
  }.build().also { file.addFunction(it) }


  FunSpec.builder(wrapKtoCefValueName).apply {
    returns(rawStruct.asCValue())
    addParameter(ParameterSpec.builder("cefStruct", kInterfaceTypeName).build())

    addStatement("return %M<%T>{ $wrapKtoCefValueName(cefStruct) }", fnCValue, rawStruct)

  }.build().also { file.addFunction(it) }
}
