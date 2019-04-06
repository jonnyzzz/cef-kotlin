package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec


private val KNRefCountedPublicFunctionParam.tmpParamName
  get() = kParamName + "C"


fun generateWrapCefToK(file : FileSpec.Builder, mapper: CefTypeMapperGenerator, info: KNRefCountedTypeInfo): Unit = info.run {

  TypeSpec.classBuilder(kWrapperTypeName).apply {
    superclass(kInterfaceTypeName)

    FunSpec.constructorBuilder().apply {
      addParameter("cefStruct", rawStruct.asCPointer())
    }.build().also { primaryConstructor(it) }

    PropertySpec.builder("cefStruct", rawStruct.asCPointer()).initializer("cefStruct").build().also { addProperty(it) }

    for (method in methods) {
      val f = FunSpec.builder(method.kFieldName).apply {
        addModifiers(KModifier.OVERRIDE)
        returns(method.kReturnType)

        for (p in method.parameters) {
          addParameter(p.kParamName, p.kParamType)
        }

        beginControlFlow("return %M", fnMemScoped)
        beginControlFlow("require(cefStruct.pointed.base.size.toInt() >= %T.size)", rawStruct)
        addStatement("%S + ", "the actual size of the ${rawStruct.simpleName} reference ")
        addStatement("%S + cefStruct.pointed.base.size + %S + %T.size", "is ", " less than ", rawStruct)
        endControlFlow()

        addStatement("val memberFunction = cefStruct.pointed.${method.cFieldName}")

        beginControlFlow("require(memberFunction != null)")
        addStatement("%S", "function ${method.cFieldName} has null pointer value in ${rawStruct.simpleName}")
        endControlFlow()

        for (param in method.parameters) {
          addCode(mapper.mapTypeFromKToCefCode(param, param.kParamName, param.tmpParamName))
        }

        addStatement("val cefResult = memberFunction.%M(cefStruct${method.parameters.joinToString("") { ", " + it.tmpParamName }})", fnInvoke)

        addCode(mapper.mapTypeFromCefToKCode(method, "cefResult", "kResult"))
        addStatement("return@memScoped kResult")
        endControlFlow()
      }.build()

      addFunction(f)
    }
  }.build().also { file.addType(it) }


  FunSpec.builder(wrapCefToKName).apply {
    addParameter("cefStruct", rawStruct.asCPointer())
    returns(kInterfaceTypeName)

    addStatement("return $kWrapperTypeName(cefStruct)")
  }.build().also { file.addFunction(it) }
}
