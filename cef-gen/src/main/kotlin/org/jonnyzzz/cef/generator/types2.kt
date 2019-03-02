package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.TypeSubstitution


fun GeneratorParameters.generateTypes2(clazzez: List<ClassDescriptor>) {
  clazzez.forEach {

    if (it.name.asString() in setOf("_cef_base_ref_counted_t", "_cef_app_t"/*, "_cef_settings_t"*/)) {
      generateType2(it)
    }

//    if (it.name.asString() != "sched_param" && it.getSuperClassNotAny()?.classId == ClassId.fromString("kotlinx/cinterop/CStructVar")) {
//      generateType(it, copyFromTypes)
//    }
  }
}


private fun GeneratorParameters.generateType2(clazz: ClassDescriptor): Unit = CefTypeInfo(clazz).run {
  val poet = FileSpec.builder(
          cefGeneratedPackage,
          "___TEST_${clazz.name.asString()}"
  )
          .addImport("kotlinx.cinterop", "cValue", "convert", "useContents", "memberAt", "ptr", "reinterpret", "invoke", "pointed")
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
          .addImport("org.jonnyzzz.cef.generated", "copyFrom")
          .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build())

  val type = TypeSpec.interfaceBuilder(kInterfaceName)
          .addAnnotation(ClassName("kotlin", "ExperimentalUnsignedTypes"))

  //do we really need that base interface explicitly?
  /*
  if (clazz.isCefBased) {
    type.addSuperinterface(CefTypeInfo(cefBaseRefCounted).kInterfaceTypeName)
  }*/

  clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
          .filter { it.shouldBePrinted }
          .filterIsInstance<PropertyDescriptor>()
          .filter { it.name.asString() !in setOf("size", "base") }
          .forEach { p ->
            val name = p.name.asString()
            val propName = name.split("_").run {
              first() + drop(1).joinToString("") { it.capitalize() }
            }

            val funType = detectFunctionPropertyType(p)
            if (funType != null) {
              val firstParam = funType.first()

              ///first parameter myst be
              if (firstParam != rawStruct.asNullableCPointer()) {
                error("First parameter of $rawStruct must be self reference!, but was $firstParam")
              }

              val fSpec = FunSpec.builder(propName)
              val fReturnType = funType.last()
              funType.dropLast(1).drop(1).forEachIndexed { idx, paramType ->
                fSpec.addParameter("p$idx",
                        when (paramType) {
                          cefString16 -> kotlinString
                          cefString16.asNullableCPointer() -> kotlinString.copy(nullable = true)
                          else -> paramType
                        })
              }

              fSpec.returns(fReturnType)
              fSpec.addModifiers(KModifier.ABSTRACT)
              type.addFunction(fSpec.build())
              return@forEach
            }
            TODO("UNSUPPORTED property ${p.javaClass} : $p")
          }

  poet.addType(type.build())

  poet.build().writeTo()
}

