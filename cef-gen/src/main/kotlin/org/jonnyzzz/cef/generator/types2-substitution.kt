package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.buildCodeBlock
import org.jonnyzzz.cef.generator.model.CefKNTypeInfo


class CefTypeSubstitution(mappedClasses: List<CefKNTypeInfo>) {
  private val cToK = mappedClasses.associate { it.rawStruct to it }

  fun mapTypeFromCefToK(type: TypeName): TypeName {
    if (type is ParameterizedTypeName && type.rawType != cPointerType) {
      val struct = type.typeArguments.single()
      val mapped = cToK[struct]
      if (mapped != null) {
        return mapped.kInterfaceTypeName.copy(nullable = type.isNullable)
      }
    }

    //TODO: include mapping of Int to Bool where possible (based on documentation analysis)
    //TODO: include mapping of cef_string_* to String (by ref parameters are possible!)
    return type
  }
}

class CefTypeMapperGenerator(
        simpleTypes: Sequence<KNSimpleTypeInfo>,
        refCounted: Sequence<KNRefCountedTypeInfo>
) {
  private val cToK = (/*simpleTypes + */refCounted).map{ it.rawStruct to it }.toMap()
  private val kToC = (/*simpleTypes + */refCounted).map{ it.kInterfaceTypeName to it }.toMap()

  private fun mapTypeFromKToCefCode(kType: TypeName, cType: TypeName, inputVal: String, outputVal: String) = buildCodeBlock {
    if (cType is ParameterizedTypeName && cType.rawType != cPointerType) {
      val struct = cType.typeArguments.single()
      val mapped = cToK[struct]
      if (mapped != null) {
        beginControlFlow("val $outputVal = if ($inputVal is %T)", mapped.kWrapperTypeName)
        if (mapped is KNRefCountedTypeInfo) {
          addStatement("$inputVal.obj.%M.base.add_ref?.%M($inputVal.obj.%M())", fnPointed, fnInvoke, fnReinterpret)
        }
        addStatement("$inputVal.obj")
        nextControlFlow("else")
        addStatement("$inputVal?.let { ${mapped.wrapKtoCefName}(it) }")
        endControlFlow()
        return@buildCodeBlock
      }
    }


    addStatement("val $outputVal = $inputVal")
  }

  private fun mapTypeFromCefToKCode(kType: TypeName, cType: TypeName, inputVal: String, outputVal: String) = buildCodeBlock {
    if (cType is ParameterizedTypeName && cType.rawType != cPointerType) {
      val struct = cType.typeArguments.single()
      val mapped = cToK[struct]
      if (mapped != null) {

        addStatement("val $outputVal = $inputVal?.let { ${mapped.wrapCefToKName}(it) }")
        return@buildCodeBlock
      }
    }


    addStatement("val $outputVal = $inputVal")
  }

  fun mapTypeFromKToCefCode(context: KNRefCountedPublicFunctionParam, inputVal: String, outputVal: String) =
          mapTypeFromKToCefCode(context.kParamType, context.cParamType, inputVal, outputVal)

  fun mapTypeFromKToCefCode(context: KNRefCountedPublicFunction, inputVal: String, outputVal: String) =
          mapTypeFromKToCefCode(context.kReturnType, context.cReturnType, inputVal, outputVal)


  fun mapTypeFromCefToKCode(context: KNRefCountedPublicFunction, inputVal: String, outputVal: String) =
          mapTypeFromCefToKCode(context.kReturnType, context.cReturnType, inputVal, outputVal)

  fun mapTypeFromCefToKCode(context: KNRefCountedPublicFunctionParam, inputVal: String, outputVal: String) =
          mapTypeFromCefToKCode(context.kParamType, context.cParamType, inputVal, outputVal)

}
