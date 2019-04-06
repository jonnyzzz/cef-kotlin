package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec


private val KNSimplePublicField.tmpFieldName
  get() = kFieldName + "C"

fun generateWrapCefToKNoBase(file : FileSpec.Builder, info: KNSimpleTypeInfo, mapper: CefTypeMapperGenerator): Unit = info.run {
  FunSpec.builder(wrapKtoCefPointerName).apply {
    returns(rawStruct.asCPointer())
    receiver(memberScopeType)
    addParameter(ParameterSpec.builder("cefStruct", kInterfaceTypeName).build())

    returnStruct(fnAlloc, info, mapper)
    addStatement("return kResult.ptr")
  }.build().also { file.addFunction(it) }


  FunSpec.builder(wrapKtoCefValueName).apply {
    returns(rawStruct.asCValue())
    addParameter(ParameterSpec.builder("cefStruct", kInterfaceTypeName).build())

    returnStruct(fnCValue, info, mapper)
    addStatement("return kResult")

  }.build().also { file.addFunction(it) }
}

private fun FunSpec.Builder.returnStruct(fnFactory: MemberName,
                                         info: KNSimpleTypeInfo,
                                         mapper: CefTypeMapperGenerator) = info.run {
  beginControlFlow("val kResult = %M<%T>", fnFactory, rawStruct)

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

  endControlFlow()
}
