package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec


fun KNRefCountedTypeInfo.generateStructWrapper() = TypeSpec.classBuilder(kStructTypeName).apply {
  addModifiers(KModifier.INTERNAL)

  superclass(ClassName("kotlinx.cinterop", "CStructVar"))

  primaryConstructor(FunSpec.constructorBuilder().addParameter("rawPtr", ClassName("kotlinx.cinterop", "NativePtr")).build())
  addSuperclassConstructorParameter("rawPtr")

  addType(TypeSpec.companionObjectBuilder()
          .superclass(ClassName("kotlinx.cinterop.CStructVar", "Type"))
          .addSuperclassConstructorParameter("%T.size + %T.size, %T.align", rawStruct, cOpaquePointerVar, rawStruct).build()
  )

  addProperty(
          PropertySpec
                  .builder("cef", rawStruct)
                  .getter(FunSpec.getterBuilder().addStatement("return %M(0)", fnMemberAt).build())
                  .build()
  )

  addProperty(
          PropertySpec
                  .builder("stablePtr", cOpaquePointerVar)
                  .getter(FunSpec.getterBuilder().addStatement("return memberAt(%T.size)", rawStruct).build())
                  .build()
  )
}
