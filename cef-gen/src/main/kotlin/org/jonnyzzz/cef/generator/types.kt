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
  println("Generate Typed Wrapper for $clazz")

  val typeName = "Cef" + clazz.name.asString().removePrefix("_").removePrefix("cef").removeSuffix("_t").split("_").joinToString("") { it.capitalize() }

  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          typeName
  )
          .addImport("kotlinx.cinterop", "cValue", "convert", "useContents", "memberAt", "ptr", "reinterpret")
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
          .addImport("org.jonnyzzz.cef.generated", "copyFrom")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

  val type = TypeSpec.classBuilder(typeName)
          .addAnnotation(ClassName("kotlin", "ExperimentalUnsignedTypes"))


  val isCefBased =
          clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
                  .filter { it.shouldBePrinted }
                  .filterIsInstance<PropertyDescriptor>()
                  .firstOrNull { it.name.asString() == "base" }?.let {
                    it.returnType?.toTypeName() == ClassName("org.jonnyzzz.cef.interop", "_cef_base_ref_counted_t")
                  } ?: false

  val (rawStruct, accessor) = if (isCefBased) {
    val companion = TypeSpec.companionObjectBuilder()
            .superclass(ClassName("kotlinx.cinterop", "CStructVar.Type"))
            .addSuperclassConstructorParameter("%T.size + 8, %T.align", clazz.toClassName(), clazz.toClassName())

    val struct = TypeSpec.classBuilder(typeName + "Raw")
    struct.primaryConstructor(FunSpec.constructorBuilder().addParameter("rawPtr", ClassName("kotlinx.cinterop", "NativePtr")).build())
    struct.superclass(ClassName("kotlinx.cinterop", "CStructVar"))
    struct.addSuperclassConstructorParameter("rawPtr")
    struct.addType(companion.build())

    struct.addProperty(PropertySpec.builder("cef", clazz.toClassName()).getter(FunSpec.getterBuilder().addStatement("return memberAt(0)").build()).build())

    poet.addType(struct.build())
    ClassName.bestGuess(struct.build().name!!) to  "cef."
  } else {
    clazz.toClassName() to ""
  }

  val cValueInit = CodeBlock.builder()
          .beginControlFlow("cValue")

  /*
  //does not work for _cef_cursor_info_t

  clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
          .filter { it.shouldBePrinted }
          .filterIsInstance<PropertyDescriptor>()
          .firstOrNull { it.name.asString() == "size" }?.let {
            cValueInit.addStatement("size = %T.size.convert()", clazz.toClassName())
          }
   */

  val structType = ParameterizedTypeName.run {
    cValueType.parameterizedBy(rawStruct)
  }

  val structRefType = ParameterizedTypeName.run {
    cPointerType.parameterizedBy(clazz.toClassName())
  }

  type.addProperty(
          PropertySpec
                  .builder("struct", structType, KModifier.PRIVATE)
                  .initializer(cValueInit.endControlFlow().build())
                  .build()
  )

  type.addProperty(PropertySpec.builder("ptr", structRefType).receiver(memberScopeType).getter(
          FunSpec.getterBuilder().addStatement("return struct.ptr.reinterpret()").build()).build())


  clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
          .filter { it.shouldBePrinted }
          .filterIsInstance<PropertyDescriptor>()
          .filter { it.name.asString() != "size" }
          .forEach { p ->
            println("property: ${p.name} : ${p.type}")
            val name = p.name.asString()
            val propName = name.split("_").run {
              first() + drop(1).joinToString("") { it.capitalize() }
            }

            val function = detectFunction(p, "X")
            if (function != null) {
              type.addModifiers(KModifier.OPEN)
            }
            println("Is is function .v.")

            if (p.type.toTypeName() == ClassName("org.jonnyzzz.cef.interop", "_cef_string_utf16_t")) {
              val prop = PropertySpec.builder(propName, String::class).mutable(true)
              prop.getter(FunSpec.getterBuilder().addStatement("return struct.useContents{ $accessor$name.asString() }").build())
              prop.setter(FunSpec.setterBuilder().addParameter("value", p.type.toTypeName()).addStatement("struct.useContents{ $accessor$name.copyFrom(value) }").build())
            } else {
              val prop = PropertySpec.builder(propName, p.type.toTypeName()).mutable(true)
              prop.getter(FunSpec.getterBuilder().addStatement("return struct.useContents{ $accessor$name }").build())
              val setter = FunSpec.setterBuilder().addParameter("value", p.type.toTypeName())
              setter.beginControlFlow("struct.useContents{ ")
              if (p.returnType in copyFromTypes) {
                setter.addStatement("$accessor$name.copyFrom(value)")
              } else {
                setter.addStatement("$accessor$name = value")
              }
              setter.endControlFlow()
              prop.setter(setter.build())
              type.addProperty(prop.build())
            }
          }


  poet.addType(type.build())
  poet.build().writeTo()
}

