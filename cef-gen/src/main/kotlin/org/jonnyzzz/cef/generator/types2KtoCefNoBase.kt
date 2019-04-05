package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec


fun generateWrapKtoCefNoBase(info: KNSimpleTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    returns(rawStruct.asCPointer())
    receiver(memberScopeType)
    addParameter(ParameterSpec.builder("obj", kInterfaceTypeName).build())

    beginControlFlow("return %M<%T>", fnAlloc, rawStruct)
    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, rawStruct, fnConvert)

    for (p in info.fields) {
      addStatement("TODO(%S)", "Implement copy for ${p.kFieldName}")
    }
    endControlFlow()
    addStatement(".ptr")
  }
}
