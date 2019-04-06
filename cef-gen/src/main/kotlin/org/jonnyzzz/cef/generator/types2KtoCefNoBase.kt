package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec


private val KNSimplePublicField.tmpFieldName
  get() = kFieldName + "C"

fun generateWrapCefToKNoBase(info: KNSimpleTypeInfo, mapper: CefTypeMapperGenerator): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    returns(rawStruct.asCPointer())
    receiver(memberScopeType)
    addParameter(ParameterSpec.builder("obj", kInterfaceTypeName).build())

    beginControlFlow("return %M<%T>", fnAlloc, rawStruct)
    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, rawStruct, fnConvert)

    //cases
    // - primitive type => assign
    // - CefRefCounted => class wrap
    // - _cef_string_^ => copyFrom()
    for (p in info.fields) {
      addCode(mapper.mapTypeFromKToCefCode(p, "obj.${p.kFieldName}", p.tmpFieldName))
    }

    for (p in info.fields) {
      when(p.cReturnType) {
        cefString16,
        cefString16.asNullableCPointer() ->
          addStatement("%M(this::${p.cFieldName}, ${p.tmpFieldName})", MemberName("org.jonnyzzz.cef", "copyCefString"))

        else ->
          addStatement("${p.cFieldName} = ${p.tmpFieldName}")
      }

    }
    endControlFlow()
    addStatement(".ptr")
  }
}
