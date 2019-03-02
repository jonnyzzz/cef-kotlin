package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isNullable


val cefInteropPackage = "org.jonnyzzz.cef.interop"
val cefGeneratedPackage = "org.jonnyzzz.cef.generated"

val memberScopeType = ClassName.bestGuess("kotlinx.cinterop.MemScope")
val cValueType = ClassName.bestGuess("kotlinx.cinterop.CValue")
val cPointerType = ClassName.bestGuess("kotlinx.cinterop.CPointer")

fun ClassifierDescriptor.toClassName(): ClassName = when(val firstParent = parents.first()) {
  is ClassDescriptor -> firstParent.toClassName().nestedClass(name.asString())
  is KonanPackageFragment -> ClassName(firstParent.fqName.asString(), name.asString())
  else -> error("Unsupported type $javaClass: $this")
}

fun KotlinType.toTypeName(): TypeName {
  val rawType = constructor.declarationDescriptor!!.toClassName().copy(isNullable())
  val args = arguments.map {
    val raw = it.type.toTypeName()

    when(it.projectionKind) {
      Variance.INVARIANT -> raw
      Variance.OUT_VARIANCE -> WildcardTypeName.producerOf(raw)
      Variance.IN_VARIANCE ->WildcardTypeName.consumerOf(raw)
    }
  }

  val finalType = if (args.isEmpty()) {
    rawType
  } else {
    ParameterizedTypeName.run {
      rawType.parameterizedBy(*args.toTypedArray())
    }
  }
  return finalType.copy(isNullable())
}

