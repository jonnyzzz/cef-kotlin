package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitution


fun GeneratorParameters.generateTypes(clazzez: List<ClassDescriptor>,
                                      copyFromTypes: Set<KotlinType>) {
  clazzez.forEach {
    if (it.name.asString() != "sched_param" && it.getSuperClassNotAny()?.classId == ClassId.fromString("kotlinx/cinterop/CStructVar")) {
      generateType(it, copyFromTypes)
    } else {
    }
  }
}


fun GeneratorParameters.generateType(clazz: ClassDescriptor,
                                     copyFromTypes: Set<KotlinType>) {
  if (clazz.name.asString() != "_cef_settings_t") return

  println("Generate Typed Wrapper for $clazz")

  val typeName = "CefSettings"

  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          typeName
  )
          .addImport("kotlinx.cinterop", "cValue", "convert", "useContents")
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
          .addImport("org.jonnyzzz.cef.generated", "copyFrom")

  val type = TypeSpec.classBuilder(typeName)

  val cValueInit = CodeBlock.builder()
          .beginControlFlow("cValue")

  clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
          .filter { it.shouldBePrinted }
          .filterIsInstance<PropertyDescriptor>()
          .firstOrNull { it.name.asString() == "size"}?.let {

            cValueInit.addStatement("size = %T.size.convert()", clazz.toClassName())

          }

  val structType = ParameterizedTypeName.run {
    ClassName.bestGuess("kotlinx.cinterop.CValue").parameterizedBy(clazz.toClassName())
  }

  val structRefType = ParameterizedTypeName.run {
    ClassName.bestGuess("kotlinx.cinterop.CPointer").parameterizedBy(clazz.toClassName())
  }

  val memberScope = ClassName.bestGuess("kotlinx.cinterop.MemScope")

  type.addProperty(
          PropertySpec
                  .builder("struct",structType, KModifier.PRIVATE)
                  .initializer(cValueInit.endControlFlow().build())
                  .build()
          )

  type.addProperty(PropertySpec.builder("ptr", structRefType).receiver(memberScope).getter(
          FunSpec.getterBuilder().addStatement("return struct.ptr").build()).build())



  clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
          .filter { it.shouldBePrinted }
          .filterIsInstance<PropertyDescriptor>()
          .filter { it.name.asString() != "size" }
          .forEach {
            println("property: ${it.name} : ${it.type}")

            val name = it.name.asString()

            val prop = if (it.type.toTypeName() == ClassName("org.jonnyzzz.cef.interop", "_cef_string_utf16_t")) {
              val prop = PropertySpec.builder(name, String::class).mutable(true)
              prop.getter(FunSpec.getterBuilder().addStatement("return struct.useContents{ $name.asString() }").build())
              prop.setter(FunSpec.setterBuilder().addParameter("value", it.type.toTypeName()).addStatement("struct.useContents{ $name.copyFrom(value) }").build())
            } else {
              val prop = PropertySpec.builder(name, it.type.toTypeName()).mutable(true)
              prop.getter(FunSpec.getterBuilder().addStatement("return struct.useContents{ $name }").build())
              val setter = FunSpec.setterBuilder().addParameter("value", it.type.toTypeName())
              setter.beginControlFlow("struct.useContents{ ")
              if (it.returnType in copyFromTypes) {
                setter.addStatement("$name.copyFrom(value)")
              } else {
                setter.addStatement("$name = value")
              }
              setter.endControlFlow()
              prop.setter(setter.build())
            }

            type.addProperty(prop.build())

          }


  poet.addType(type.build())
  poet.build().writeTo()
}

