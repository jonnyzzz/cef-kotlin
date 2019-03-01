package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

fun ClassifierDescriptor.toClassName() =
   ClassName(parents.firstIsInstance<KonanPackageFragment>().fqName.asString(), name.asString())

fun KotlinType.toTypeName(): TypeName {
  val rawType  = constructor.declarationDescriptor!!.toClassName()

  val args = arguments.map { it.type.toTypeName() }
  return if (args.isEmpty()) rawType else ParameterizedTypeName.run { rawType.parameterizedBy(*args.toTypedArray()) }
}

