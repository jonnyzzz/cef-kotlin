package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import org.jonnyzzz.cef.generator.model.wrapKtoCefName

interface KNSimpleField {
  val memberName: String
  val returnType: TypeName
}

interface KNSimpleTypeInfo {
  val rawStruct: ClassName
  val kInterfaceTypeName : ClassName

  val fields : List<KNSimpleField>
}

fun generateWrapKtoCefNoBase2(info: KNSimpleTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    returns(rawStruct.asCPointer())
    receiver(kInterfaceTypeName)
    addParameter("scope", memberScopeType)

    addStatement("return $wrapKtoCefName(this).getPointer(scope)")
  }
}


fun generateWrapKtoCefNoBase(info: KNSimpleTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    returns(rawStruct.asCValue())
    addParameter(ParameterSpec.builder("obj", kInterfaceTypeName).build())

    addStatement("val cValue = ", rawStruct)
    addCode(generateCValueInitBlockCefNoBase(info).build())
    addStatement("return cValue")
  }
}

private fun generateCValueInitBlockCefNoBase(info: KNSimpleTypeInfo): CodeBlock.Builder = info.run {
  CodeBlock.builder().apply {

    beginControlFlow("%M<%T>", fnCValue, rawStruct)
    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, rawStruct, fnConvert)

    for (p in info.fields) {
      val value = "obj.${p.memberName}"
/*

      //TODO: hide inside FieldPropertyDescriptor!
      if (p.originalTypeName ?: p.propType in copyFromTypeNames) {
        addStatement("${p.cFieldName}.copyFrom($value)")
      } else {
        addStatement("${p.cFieldName} = $value")
      }
*/
    }

    endControlFlow()
  }
}
