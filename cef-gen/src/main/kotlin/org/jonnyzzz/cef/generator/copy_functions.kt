package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance


val copyMethodName = "copyFrom"


fun generateCopyFunctions(clazzez: List<ClassDescriptor>) {
  val cefAppT = clazzez.filterIsInstance<ClassDescriptor>().first { it.name.asString() == "_cef_app_t" }

  generateCopyFunction(cefAppT)
}


fun generateCopyFunction(clazz: ClassDescriptor) {
  val className = ClassName(clazz.parents.firstIsInstance<KonanPackageFragment>().fqName.asString(), clazz.name.asString())


  val poet = FileSpec.builder(
          "org.jonnyzzz.cef.generated",
          "generated.kt"
  ).addFunction(
          FunSpec.builder(copyMethodName)
                  .addModifiers(KModifier.INLINE)
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

  println(poet)

}