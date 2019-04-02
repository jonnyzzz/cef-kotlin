package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jonnyzzz.cef.generator.c.CefDeclarations
import org.jonnyzzz.cef.generator.model.CefKNTypeInfo
import java.io.File

data class GeneratorParameters(
        val cefIncludesDir: File,
        val outputDir: File
) {
  fun FileSpec.Builder.writeTo(suffix: String = "") = build().writeTo(suffix)

  fun FileSpec.writeTo(suffix: String = "") {
    val target = if(suffix.isEmpty()) outputDir else outputDir / suffix
    target.mkdirs()
    writeTo(target)
  }

  lateinit var cefDeclarations: CefDeclarations

  lateinit var enumTypes : Set<TypeName>
  lateinit var copyFromTypes: Set<KotlinType>
  val copyFromTypeNames: Set<TypeName> get() = copyFromTypes.map { it.toTypeName() }.toSet()

  lateinit var cefBaseClassDescriptor: ClassDescriptor
  lateinit var cefBaseClassDescriptorInfo: CefKNTypeInfo
}
