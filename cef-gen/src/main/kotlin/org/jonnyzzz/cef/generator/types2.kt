package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
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

    if (it.name.asString() in setOf("_cef_base_ref_counted_t", "_cef_app_t", "_cef_before_download_callback_t", "_cef_settings_t")) {
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
                          .getter(FunSpec.getterBuilder().addStatement("return memberAt(0)").build())
                          .build()
          )

          // val stablePtr : COpaquePointerVar
          //        get() = memberAt(_cef_before_download_callback_t.size)
          .addProperty(
                  PropertySpec
                          .builder("stablePtr", cOpaquePointerVar)
                          .getter(FunSpec.getterBuilder().addStatement("return memberAt(%T.size)", rawStruct).build())
                  .build()
          )
}

private fun GeneratorParameters.generateImplBase(info: CefKNTypeInfo, clazz: ClassDescriptor) : TypeSpec.Builder = info.run {
  val cValueInit = CodeBlock.builder()
          .beginControlFlow("cValue")
          .addStatement("memset(ptr, 0, %T.size.convert())", kStructTypeName)
          .apply {
            when {
              clazz.isCefBased -> addStatement("cef.base.size = %T.size.convert()", kStructTypeName)
              else -> addStatement("cef.size = %T.size.convert()", kStructTypeName)
            }
          }
          .addStatement("stablePtr.value = stableRef.asCPointer()")
          .also { code ->
            for (p in info.functionProperties) {
              code.beginControlFlow("cef.${p.cFieldName} = staticCFunction")
              code.addStatement(
                      (listOf(p.THIS) + p.parameters).joinToString(", ") { it.paramName } + " ->"
              )

              //   val pThis = THIS!!.reinterpret<KCefBeforeDownloadCallbackStruct>()
              //                    .pointed
              //                    .stablePtr
              //                    .value!!
              //                    .asStableRef<KCefBeforeDownloadCallbackBase>().get()

              code.addStatement("val pThis = ${p.THIS.paramName}!!.reinterpret<%T>()", kStructTypeName)
              code.indent().indent()
              code.addStatement(".pointed")
              code.addStatement(".stablePtr")
              code.addStatement(".value!!")
              code.addStatement(".asStableRef<%T>()", kImplBaseTypeName)
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
                      code.addStatement("safe_cef_string_clear(cef.${p.name}.ptr)")
                    }
          }
          .endControlFlow()
          .build()


  TypeSpec.classBuilder(kImplBaseTypeName)
          .addModifiers(KModifier.ABSTRACT)
          .addSuperinterface(kInterfaceTypeName)
          .primaryConstructor(
                  FunSpec.constructorBuilder()
                          .addParameter("defer", ClassName("kotlinx.cinterop","DeferScope"))
                          .build()
          ).apply {
            if (clazz.isCefBased) {
              addSuperinterface(cefBaseClassDescriptorInfo.kInterfaceTypeName, CodeBlock.of("%T()", cefBaseRefCountedKImpl))
            }
          }

          .addProperty(PropertySpec
                  .builder("ptr", rawStruct.asNullableCPointer())
                  .receiver(memberScopeType)
                  .getter(FunSpec
                          .getterBuilder()
                          .addStatement("return cValue.ptr.reinterpret()")
                          .build()
                  ).build()
          )

          //private val stableRef = StableRef.create(this).also { defer.defer { it.dispose() } }
          .addProperty(PropertySpec
                  .builder("stableRef", ParameterizedTypeName.run { stableRef.parameterizedBy(kImplBaseTypeName) })
                  .addModifiers(KModifier.PRIVATE)
                  .initializer("%T.create(this).also { defer.defer { it.dispose() } }", stableRef)
                  .build()
                  )

          .addProperty(PropertySpec
                  .builder("cValue", kStructTypeName.asCValue())
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
                              .beginControlFlow("cValue.useContents")
                              .addStatement("return " + p.fromCefToKotlin("cef.${p.cFieldName}"))
                              .endControlFlow()
                              .build()
                      )
                      .setter(FunSpec
                              .setterBuilder()
                              .addParameter("value", p.propType)
                              .beginControlFlow("cValue.useContents")
                              .apply {
                                if (p.originalTypeName?: p.propType in copyFromTypeNames) {
                                  addStatement("cef.${p.cFieldName}.copyFrom(value)")
                                } else {
                                  addStatement("cef.${p.cFieldName} = value")
                                }
                              }
                              .endControlFlow()
                              .build()
                      )


              type.addProperty(spec.build())
            }

          }

}

private fun GeneratorParameters.generateType2(clazz: ClassDescriptor): Unit = cefTypeInfo(clazz).run {
  val poet = FileSpec.builder(
          cefGeneratedPackage,
          sourceFileName
  )
          .addImport("kotlinx.cinterop", "cValue", "value", "convert", "useContents", "memberAt", "ptr", "reinterpret", "invoke", "pointed", "staticCFunction", "asStableRef")
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
          .addImport("org.jonnyzzz.cef.generated", "copyFrom")
          .addImport("platform.posix", "memset")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

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
    fSpec.addModifiers(KModifier.ABSTRACT)
    kInterface.addFunction(fSpec.build())
  }

  fieldProperties.filter { it.visibleInInterface }.forEach { p ->
    val pSpec = PropertySpec.builder(p.propName, p.propType).mutable(true).addKdoc(p)
    kInterface.addProperty(pSpec.build())
  }

  poet.addType(kInterface.build())

  poet.addType(generateStructWrapper(this).build())
  poet.addType(generateImplBase(this, clazz).build())

  poet.build().writeTo()
}
