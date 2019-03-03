package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import java.io.File

data class GeneratorParameters(val outputDir: File) {
  fun FileSpec.writeTo() = writeTo(outputDir)

  lateinit var enumTypes : Set<TypeName>
  lateinit var copyFromTypes: Set<KotlinType>

  lateinit var cefBaseClassDescriptor: ClassDescriptor
}
