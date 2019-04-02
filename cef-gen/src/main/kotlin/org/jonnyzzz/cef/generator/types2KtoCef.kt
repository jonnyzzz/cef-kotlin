package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jonnyzzz.cef.generator.kn.CefKNTypeInfo
import org.jonnyzzz.cef.generator.kn.FunctionalPropertyDescriptor
import org.jonnyzzz.cef.generator.kn.fromCefToKotlin

fun CefKNTypeInfo.generateStructWrapper(): TypeSpec.Builder {
  return TypeSpec.classBuilder(kStructTypeName)
          .addModifiers(KModifier.INTERNAL)
          .primaryConstructor(FunSpec.constructorBuilder().addParameter("rawPtr", ClassName("kotlinx.cinterop", "NativePtr")).build())
          .superclass(ClassName("kotlinx.cinterop", "CStructVar"))
          .addSuperclassConstructorParameter("rawPtr")
          .addType(TypeSpec.companionObjectBuilder()
                  .superclass(ClassName("kotlinx.cinterop.CStructVar", "Type"))
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


fun GeneratorParameters.generateWrapKtoCef2(info: CefKNTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    returns(rawStruct.asCPointer())
    receiver(kInterfaceTypeName)

    addStatement("return $wrapKtoCefName(this)")
  }
}

fun GeneratorParameters.generateWrapKtoCef(info: CefKNTypeInfo): FunSpec.Builder = info.run {
  FunSpec.builder(wrapKtoCefName).apply {
    require(cefBaseTypeInfo != null) { "type $rawStruct must not be CefBased!"}

    returns(rawStruct.asCPointer())
    addParameter(ParameterSpec.builder("obj", kInterfaceTypeName).build())

    if (cefBaseTypeInfo != null) {
      addStatement("val scope = %T()", arenaType)
    } else {
      addStatement("val scope = this")
      receiver(memberScopeType)
    }

    addStatement("val stableRef = scope.%M(%T(scope, obj))", fnCefStablePrt, cefBaseRefCountedKImpl)

    addStatement("val cValue = ", rawStruct)
    addCode(generateCValueInitBlock(info).build())

    addStatement("return cValue.%M<%T>().ptr", fnReinterpret, rawStruct)
  }
}


private fun generateTHISUnwrap(into: CefKNTypeInfo, p: FunctionalPropertyDescriptor): CodeBlock.Builder = into.run {
  CodeBlock.builder().apply {
    addStatement("initRuntimeIfNeeded()")
    add("val pThis = ${p.THIS.paramName}")
    add("?.%M<%T>()", fnReinterpret, kStructTypeName)
    add("?.%M?.stablePtr?.value", fnPointed)
    add("?.%M<%T>()?.get()", fnAsStableRef, cefBaseRefCountedKImpl.parameterizedBy(kInterfaceTypeName))
    add("?: error(%S)", "THIS == null for $rawStruct#${p.funName}")
    addStatement("")
  }
}

private fun GeneratorParameters.generateCValueInitBlock(info: CefKNTypeInfo): CodeBlock.Builder = info.run {
  CodeBlock.builder().apply {
    beginControlFlow("scope.%M<%T>", fnAlloc, kStructTypeName)
    addStatement("%M(%M, 0, %T.size.%M())", fnPosixMemset, fnPtr, kStructTypeName, fnConvert)
    addStatement("cef.base.size = %T.size.%M()", kStructTypeName, fnConvert)
    addStatement("stablePtr.%M = stableRef.asCPointer()", fnValue)

    require(info.fieldProperties.isEmpty()) { "type $rawStruct must not be non-functional fields!"}

    for (p in info.functionProperties) {
      beginControlFlow("cef.${p.cFieldName} = %M", fnStaticCFunction)
      addStatement((listOf(p.THIS) + p.parameters).joinToString(", ") { it.paramName } + " ->")
      add(generateTHISUnwrap(info, p).build())
      addStatement("pThis.obj.${p.funName}(${p.parameterNamesList})")
      endControlFlow()
    }

    //TODO: merge code!
    cefBaseTypeInfo?.let { cefBase ->
      addStatement("// CEF Base Implementation")
      for (p in cefBase.functionProperties) {
        beginControlFlow("cef.base.${p.cFieldName} = %M", fnStaticCFunction)
        addStatement((listOf(p.THIS) + p.parameters).joinToString(", ") { it.paramName } + " ->")
        add(generateTHISUnwrap(info, p).build())
        addStatement("pThis.${p.funName}(${p.parameterNamesList})")
        endControlFlow()
      }

      require(cefBase.fieldProperties.isEmpty())
    }

    endControlFlow()
  }
}

fun GeneratorParameters.generateImplBase(info: CefKNTypeInfo): TypeSpec.Builder = info.run {
  TypeSpec.classBuilder(kImplBaseTypeName)
          .addModifiers(KModifier.ABSTRACT)
          .addSuperinterface(kInterfaceTypeName)
          .primaryConstructor(
                  FunSpec.constructorBuilder()
                          .addParameter("scope", ClassName("kotlinx.cinterop", "MemScope"))
                          .build()
          ).apply {
            info.cefBaseTypeInfo?.let { baseInfo ->
              addSuperinterface(baseInfo.kInterfaceTypeName, CodeBlock.of("%T()", cefBaseRefCountedKImpl))
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
                  .initializer(generateCValueInitBlock(info).build())
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
                                //TODO: hide inside FieldPropertyDescriptor!
                                if (p.originalTypeName ?: p.propType in copyFromTypeNames) {
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
