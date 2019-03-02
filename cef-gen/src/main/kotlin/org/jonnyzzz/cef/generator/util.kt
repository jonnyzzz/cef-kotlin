package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithNothing
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

fun ClassifierDescriptor.toClassName() =
        ClassName(parents.firstIsInstance<KonanPackageFragment>().fqName.asString(), name.asString())

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

