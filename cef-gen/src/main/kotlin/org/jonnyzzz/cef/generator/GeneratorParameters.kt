package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jonnyzzz.cef.generator.c.CefDeclarations
import java.io.File

data class GeneratorParameters(
        val cefIncludesDir: File,
        val outputDir: File
) {
  fun FileSpec.writeTo() = writeTo(outputDir)

  lateinit var cefDeclarations: CefDeclarations

  lateinit var enumTypes : Set<TypeName>
  lateinit var copyFromTypes: Set<KotlinType>
  val copyFromTypeNames: Set<TypeName> get() = copyFromTypes.map { it.toTypeName() }.toSet()

  lateinit var cefBaseClassDescriptor: ClassDescriptor
}
