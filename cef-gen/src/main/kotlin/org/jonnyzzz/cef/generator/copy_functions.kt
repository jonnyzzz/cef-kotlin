package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitution


private const val copyMethodName = "copyFrom"


fun GeneratorParameters.generateCopyFunctions(clazzez: List<ClassDescriptor>): Set<KotlinType> {
  return clazzez.mapNotNull {

    if (it.name.asString() != "sched_param" && it.getSuperClassNotAny()?.classId == ClassId.fromString("kotlinx/cinterop/CStructVar")) {
      println("${it.name.asString()} - generate")
      generateCopyFunction(it)
    } else {
      println("${it.name.asString()} - SKIP")
      null
    }
  }.toSet()
}


fun GeneratorParameters.generateCopyFunction(clazz: ClassDescriptor): KotlinType? {
  val className = clazz.toClassName()


  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          "copy_" + className.simpleName
  ).addFunction(
          FunSpec.builder(copyMethodName)
                  .addModifiers(KModifier.INLINE)
                  .addAnnotation(AnnotationSpec.builder(Suppress::class)
                          .addMember("%S", "NOTHING_TO_INLINE")
                          .build()
                  )
                  .addAnnotation(ClassName("kotlin", "ExperimentalUnsignedTypes"))
                  .addKdoc("Performs deep copy of all\n" +
                           "fields of the [target] structure\n" +
                           "into the receiver structure")
                  .receiver(className)
                  .addParameter("target", className)
                  .apply {
                    clazz.getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
                            .filter { it.shouldBePrinted }
                            .filterIsInstance<PropertyDescriptor>()
                            .forEach {
                              val prop = it.name.asString()
                              if (it.isVar) {
                                addStatement("this.$prop = target.$prop")
                              } else {
                                addStatement("this.$prop.$copyMethodName(target.$prop)")
                              }
                            }
                  }
                  .build()
  ).build()

  poet.writeTo(outputDir)

  return clazz.defaultType
}