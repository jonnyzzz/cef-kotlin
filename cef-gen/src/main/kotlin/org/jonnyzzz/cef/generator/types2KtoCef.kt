package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.kn.CefKNTypeInfo
import org.jonnyzzz.cef.generator.kn.allMeaningfulProperties
import org.jonnyzzz.cef.generator.kn.fromCefToKotlin
import org.jonnyzzz.cef.generator.kn.isCefBased

fun CefKNTypeInfo.generateStructWrapper() : TypeSpec.Builder {
  return TypeSpec.classBuilder(kStructTypeName)
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


fun GeneratorParameters.generateImplBase(info: CefKNTypeInfo, clazz: ClassDescriptor) : TypeSpec.Builder = info.run {
  val cValueInit = CodeBlock.builder()
          .beginControlFlow("scope.%M", MemberName("kotlinx.cinterop", "alloc"))
          .addStatement("%M(ptr, 0, %T.size.%M())", fnPosixMemset, kStructTypeName, fnConvert)
          .apply {
            when {
              info.isCefBased -> addStatement("cef.base.size = %T.size.%M()", kStructTypeName, fnConvert)
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
            if (info.isCefBased) {
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
