package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.kn.CefKNTypeInfo
import org.jonnyzzz.cef.generator.kn.addKdoc
import org.jonnyzzz.cef.generator.kn.cefTypeInfo
import org.jonnyzzz.cef.generator.kn.allMeaningfulProperties
import org.jonnyzzz.cef.generator.kn.fromCefToKotlin
import org.jonnyzzz.cef.generator.kn.isCefBased


fun GeneratorParameters.generateTypes2(clazzez: List<ClassDescriptor>) {
  clazzez.forEach {

    if (it.name.asString() in setOf("_cef_browser_process_handler_t", "_cef_base_ref_counted_t", "_cef_app_t", "_cef_before_download_callback_t", "_cef_settings_t", "_cef_client_t", "_cef_window_info_t", "_cef_browser_settings_t")) {
      generateType2(it)
    }

//    if (it.name.asString() != "sched_param" && it.getSuperClassNotAny()?.classId == ClassId.fromString("kotlinx/cinterop/CStructVar")) {
//      generateType(it, copyFromTypes)
//    }
  }
}

private fun GeneratorParameters.generateStructWrapper(info: CefKNTypeInfo) : TypeSpec.Builder = info.run {
  TypeSpec.classBuilder(kStructTypeName)
          .addModifiers(KModifier.PRIVATE)
          .primaryConstructor(FunSpec.constructorBuilder().addParameter("rawPtr", ClassName("kotlinx.cinterop", "NativePtr")).build())
          .superclass(ClassName("kotlinx.cinterop", "CStructVar"))
          .addSuperclassConstructorParameter("rawPtr")
          .addType(TypeSpec.companionObjectBuilder()
                  .superclass(ClassName("kotlinx.cinterop", "CStructVar.Type"))
                  .addSuperclassConstructorParameter("%T.size + %T.size, %T.align", rawStruct, cOpaquePointerVar, rawStruct).build()
          )

          .addProperty(
                  PropertySpec
                          .builder("cef", rawStruct)
                          .getter(FunSpec.getterBuilder().addStatement("return %M(0)", fnMemberAt).build())
                          .build()
          )

          .addProperty(
                  PropertySpec
                          .builder("stablePtr", cOpaquePointerVar)
                          .getter(FunSpec.getterBuilder().addStatement("return memberAt(%T.size)", rawStruct).build())
                  .build()
          )
}

private fun GeneratorParameters.generateImplBase(info: CefKNTypeInfo, clazz: ClassDescriptor) : TypeSpec.Builder = info.run {
  val cValueInit = CodeBlock.builder()
          .beginControlFlow("scope.%M", MemberName("kotlinx.cinterop", "alloc"))
          .addStatement("%M(ptr, 0, %T.size.%M())", fnPosixMemset, kStructTypeName, fnConvert)
          .apply {
            when {
              clazz.isCefBased -> addStatement("cef.base.size = %T.size.%M()", kStructTypeName, fnConvert)
              //TODO: resolve `size` field via library scan instead
              info.kInterfaceTypeName.simpleName == "KCefWindowInfo" -> {}
              else -> addStatement("cef.size = %T.size.%M()", kStructTypeName, fnConvert)
            }
          }
          .addStatement("stablePtr.%M = stableRef.asCPointer()", fnValue)
          .also { code ->
            for (p in info.functionProperties) {
              code.beginControlFlow("cef.${p.cFieldName} = %M", fnStaticCFunction)
              code.addStatement(
                      (listOf(p.THIS) + p.parameters).joinToString(", ") { it.paramName } + " ->"
              )

              code.addStatement("initRuntimeIfNeeded()")

              code.addStatement("val pThis = ${p.THIS.paramName}!!.%M<%T>()", fnReinterpret, kStructTypeName)
              code.indent().indent()
              code.addStatement(".%M", fnPointed)
              code.addStatement(".stablePtr")
              code.addStatement(".value!!")
              code.addStatement(".%M<%T>()", fnAsStableRef, kImplBaseTypeName)
              code.addStatement(".get()")
              code.unindent().unindent()
              code.addStatement("")
              code.addStatement("pThis.${p.funName}(" +
                      p.parameters.joinToString(", ") { it.fromCefToKotlin(it.paramName) } +
                      ")")
              code.endControlFlow()
              code.addStatement("")
            }
          }
          .also { code ->
            clazz.allMeaningfulProperties()
                    .filter { !it.isVar}
                    .filter { it.type.toTypeName() == ClassName("org.jonnyzzz.cef.interop", "_cef_string_utf16_t")}
                    .forEach { p ->
                      code.addStatement("cefStringClear(cef.${p.name}.ptr)")
                    }
          }
          .endControlFlow()
          .build()


  TypeSpec.classBuilder(kImplBaseTypeName)
          .addModifiers(KModifier.ABSTRACT)
          .addSuperinterface(kInterfaceTypeName)
          .primaryConstructor(
                  FunSpec.constructorBuilder()
                          .addParameter("scope", ClassName("kotlinx.cinterop", "MemScope"))
                          .build()
          ).apply {
            if (clazz.isCefBased) {
              addSuperinterface(cefBaseClassDescriptorInfo.kInterfaceTypeName, CodeBlock.of("%T()", cefBaseRefCountedKImpl))
            }
          }

          .addProperty(PropertySpec
                  .builder("ptr", rawStruct.asCPointer())
                  .getter(FunSpec
                          .getterBuilder()
                          .addStatement("return cValue.%M<%T>().%M", fnReinterpret, rawStruct, MemberName("kotlinx.cinterop", "ptr"))
                          .build()
                  ).build()
          )

          .addProperty(PropertySpec
                  .builder("stableRef", ParameterizedTypeName.run { stableRef.parameterizedBy(kImplBaseTypeName) })
                  .addModifiers(KModifier.PRIVATE)
                  .initializer("scope.%M(this)", MemberName("org.jonnyzzz.cef.internal", "stablePtr"))
                  .build()
                  )

          .addProperty(PropertySpec
                  .builder("cValue", kStructTypeName)
                  .addModifiers(KModifier.PRIVATE)
                  .initializer(cValueInit)
                  .build()
          )


          .also { type ->
            info.fieldProperties.forEach { p ->
              val spec = PropertySpec
                      .builder(p.propName, p.propType, KModifier.OVERRIDE).mutable(true)
                      .getter(FunSpec
                              .getterBuilder()
                              .addStatement("return cValue." + p.fromCefToKotlin("cef.${p.cFieldName}"))
                              .build()
                      )
                      .setter(FunSpec
                              .setterBuilder()
                              .addParameter("value", p.propType)
                              .apply {
                                if (p.originalTypeName?: p.propType in copyFromTypeNames) {
                                  addStatement("cValue.cef.${p.cFieldName}.copyFrom(value)")
                                } else {
                                  addStatement("cValue.cef.${p.cFieldName} = value")
                                }
                              }
                              .build()
                      )


              type.addProperty(spec.build())
            }

          }

}

private fun CefKNTypeInfo.generateKInterface(): TypeSpec.Builder {
  val kInterface = TypeSpec.interfaceBuilder(kInterfaceTypeName).addKdoc(this)

  //do we really need that base interface explicitly?
  /*
  if (clazz.isCefBased) {
    type.addSuperinterface(cefTypeInfo(cefBaseRefCounted).kInterfaceTypeName)
  }*/

  functionProperties.filter { it.visibleInInterface }.forEach { p ->
    val fSpec = FunSpec.builder(p.funName).addKdoc(p)

    p.parameters.forEach {
      fSpec.addParameter(it.paramName, it.paramType)
    }

    fSpec.returns(p.returnType)

    //default implementation for nullable types
    if (p.returnType.isNullable) {
      fSpec.addStatement("return null")
    } else {
      fSpec.addModifiers(KModifier.ABSTRACT)
    }

    kInterface.addFunction(fSpec.build())
  }

  fieldProperties.filter { it.visibleInInterface }.forEach { p ->
    val pSpec = PropertySpec.builder(p.propName, p.propType).mutable(true).addKdoc(p)
    kInterface.addProperty(pSpec.build())
  }
  return kInterface
}


private fun GeneratorParameters.generateType2(clazz: ClassDescriptor): Unit = cefTypeInfo(clazz).run {
  val interfaceFile = FileSpec.builder(
          cefGeneratedPackage,
          sourceInterfaceFileName
  )

  interfaceFile.addType(generateKInterface().build())
  interfaceFile.build().writeTo()


  val kotlinToCefFile = FileSpec.builder(
          cefGeneratedPackage,
          sourceKtoCefFileName
  )
//  )
//          .addImport("kotlinx.cinterop", "alloc", "cValue", "value", "convert", "useContents", "memberAt", "ptr", "reinterpret", "invoke", "pointed", "staticCFunction", "asStableRef")
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
//          .addImport("org.jonnyzzz.cef.internal", "stablePtr")
//          .addImport("org.jonnyzzz.cef.generated", "copyFrom")
//          .addImport("kotlin.native.concurrent", "isFrozen")
//          .addImport("kotlin.native", "initRuntimeIfNeeded")
//          .addImport("platform.posix", "memset")
//          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())


  kotlinToCefFile.addType(generateStructWrapper(this).build())
  kotlinToCefFile.addType(generateImplBase(this, clazz).build())
  kotlinToCefFile.build().writeTo()
}
