package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
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


fun GeneratorParameters.generateTypes(clazzez: List<ClassDescriptor>) {
  clazzez.forEach {
    if (it.name.asString() != "sched_param" && it.getSuperClassNotAny()?.classId == ClassId.fromString("kotlinx/cinterop/CStructVar")) {
      generateType(it, copyFromTypes)
    }
  }
}

fun GeneratorParameters.generateType(clazz: ClassDescriptor,
                                     copyFromTypes: Set<KotlinType>) : Unit = CefTypeInfo(clazz).run {
  println("Generate Typed Wrapper for $clazz")

  val poet = FileSpec.builder(
          cefGeneratedPackage,
          typeName
  )
          .addImport("kotlinx.cinterop", "cValue", "convert", "useContents", "memberAt", "ptr", "reinterpret", "invoke", "pointed")
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
          .addImport("org.jonnyzzz.cef.generated", "copyFrom")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

  val type = TypeSpec.interfaceBuilder(typeName)

  /*
  .primaryConstructor(FunSpec.constructorBuilder()
          .addParameter("struct", structType)
          .build())
  .addProperty(PropertySpec.builder("struct", structType)
          .initializer("struct")
          .build())
*/

  if (clazz.isCefBased) {
    val cefBaseInfo = CefTypeInfo(cefBaseRefCounted)
    type.addSuperinterface(cefBaseInfo.typeClassName)

    type.addProperty(PropertySpec
            .builder(cefBaseInfo.pointedName, cefBaseInfo.rawStruct, KModifier.OVERRIDE)
            .getter(FunSpec.getterBuilder().addStatement("return $pointedName.base").build())
            .build()
    )
  }


/*
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
    ClassName.bestGuess(struct.build().name!!) to "cef."
  } else {
    clazz.toClassName() to ""
  }

  val cValueInit = CodeBlock.builder()
          .beginControlFlow("cValue")

  //does not work for _cef_cursor_info_t

  clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
          .filter { it.shouldBePrinted }
          .filterIsInstance<PropertyDescriptor>()
          .firstOrNull { it.name.asString() == "size" }?.let {
            cValueInit.addStatement("size = %T.size.convert()", clazz.toClassName())
          }
   */

/*

  type.addProperty(
          PropertySpec
                  .builder("struct", structType, KModifier.PRIVATE)
                  .initializer(cValueInit.endControlFlow().build())
                  .build()
  )

  type.addProperty(
          PropertySpec.builder("ptr", structRefType).receiver(memberScopeType).getter(
          FunSpec.getterBuilder().addStatement("return struct.ptr.reinterpret()").build()).build())

*/

  type.addProperty(
          PropertySpec.builder(pointedName, rawStruct).build()
  )

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

            val function = detectFunctionProperty(p, propName)
            if (function != null) {
              val (funcSpec, params, returnType) = function
              when {
                returnType in enumTypes -> funcSpec.addStatement("TODO(%S)", "cinterop does not support enum C function return types (e.g. in 1.3.21)")
                returnType is ParameterizedTypeName && returnType.rawType == ClassName("kotlinx.cinterop", "CValue") -> funcSpec.addStatement("TODO(%S)", "cinterop does not support CValue<*> C function return types (e.g. in 1.3.21)")
                else -> funcSpec.addStatement("return $pointedName.$name!!.invoke(${params.joinToString(", ") { it.paramName }})")
              }

              type.addFunction(funcSpec.build())
              return@forEach
            }

            if (p.type.toTypeName() == ClassName(cefInteropPackage, "_cef_string_utf16_t")) {
              val prop = PropertySpec.builder(propName, String::class).mutable(true)
              prop.getter(FunSpec.getterBuilder()
                      .addStatement("return $pointedName.$name.asString()")
                      .build()
              )
              prop.setter(FunSpec.setterBuilder()
                      .addParameter("value", p.type.toTypeName())
                      .addStatement("$pointedName.$name.copyFrom(value)")
                      .build())

              type.addProperty(prop.build())
              return@forEach
            }

            val prop = PropertySpec.builder(propName, p.type.toTypeName()).mutable(true)
            prop.getter(FunSpec.getterBuilder()
                    .addStatement("return $pointedName.$name")
                    .build()
            )
            val setter = FunSpec.setterBuilder().addParameter("value", p.type.toTypeName())
            when {
              p.returnType in copyFromTypes ->
                setter.addStatement("$pointedName.$name.copyFrom(value)")

              else ->
                setter.addStatement("$pointedName.$name = value")
            }
            prop.setter(setter.build())
            type.addProperty(prop.build())
          }


  poet.addType(type.build())
  poet.build().writeTo()
}

