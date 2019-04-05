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
      addParameter("obj", rawStruct.asCPointer())
    }.build().also { primaryConstructor(it) }

    PropertySpec.builder("obj", rawStruct.asCPointer()).initializer("obj").build().also { addProperty(it) }

    for (method in methods) {
      val f = FunSpec.builder(method.kFieldName).apply {
        addModifiers(KModifier.OVERRIDE)
        returns(method.kReturnType)

        for (p in method.parameters) {
          addParameter(p.kParamName, p.kParamType)
        }

        beginControlFlow("require(obj.pointed.base.size.toInt() >= %T.size)", rawStruct)
        addStatement("%S + ", "the actual size of the ${rawStruct.simpleName} reference ")
        addStatement("%S + obj.pointed.base.size + %S + %T.size", "is ", " less than ", rawStruct)
        endControlFlow()

        addStatement("val handler = obj.pointed.${method.cFieldName}")
        addStatement("⇥⇥?: return super.${method.kFieldName}(${method.parameters.joinToString(", ") { it.kParamName }})⇤⇤")

        for (param in method.parameters) {
          addCode(mapper.mapTypeFromKToCefCode(param, param.kParamName, param.tmpParamName))
        }

        addStatement("val cefResult = handler.%M(obj${method.parameters.joinToString("") { ", " + it.tmpParamName }})", fnInvoke)

        addCode(mapper.mapTypeFromCefToKCode(method, "cefResult", "kResult"))
        addStatement("return kResult")
      }.build()

      addFunction(f)
    }
  }.build().also { file.addType(it) }


  FunSpec.builder(wrapCefToKName).apply {
    addParameter("obj", rawStruct.asCPointer())
    returns(kInterfaceTypeName)

    addStatement("return $kWrapperTypeName(obj)")
  }.build().also { file.addFunction(it) }
}
