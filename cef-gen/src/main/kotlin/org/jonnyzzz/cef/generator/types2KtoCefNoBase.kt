package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import org.jonnyzzz.cef.generator.kn.CefKNTypeInfo


fun GeneratorParameters.generateWrapKtoCefNoBase2(info: CefKNTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    returns(rawStructPointer)
    receiver(kInterfaceTypeName)
    addParameter("scope", memberScopeType)

    addStatement("return $wrapKtoCefName(this).getPointer(scope)")
  }
}


fun GeneratorParameters.generateWrapKtoCefNoBase(info: CefKNTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    require(!isCefBased) { "type $rawStruct must not be CefBased!"}

    returns(rawStruct.asCValue())
    addParameter(ParameterSpec.builder("obj", kInterfaceTypeName).build())

    addStatement("val cValue = ", rawStruct)
    addCode(generateCValueInitBlockCefNoBase(info).build())
    addStatement("return cValue")
  }
}

private fun GeneratorParameters.generateCValueInitBlockCefNoBase(info: CefKNTypeInfo): CodeBlock.Builder = info.run {
  CodeBlock.builder().apply {
    require(!isCefBased) { "type $rawStruct must not be CefBased!"}

    beginControlFlow("%M<%T>", fnCValue, rawStruct)
    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, rawStruct, fnConvert)

    when {
      info.kInterfaceTypeName.simpleName == "KCefWindowInfo" -> { }
      else -> addStatement("size = %T.size.%M()", rawStruct, fnConvert)
    }

    require(info.functionProperties.isEmpty()) { "type $rawStruct must not have functions!"}

    for (p in info.fieldProperties) {
      val value = "obj.${p.propName}"

      //TODO: hide inside FieldPropertyDescriptor!
      if (p.originalTypeName ?: p.propType in copyFromTypeNames) {
        addStatement("${p.cFieldName}.copyFrom($value)")
      } else {
        addStatement("${p.cFieldName} = $value")
      }
    }

    endControlFlow()
  }
}
