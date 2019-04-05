package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isNullable


val cefInteropPackage = "org.jonnyzzz.cef.interop"
val cefGeneratedPackage = "org.jonnyzzz.cef.generated"

val cefString16 = ClassName(cefInteropPackage, "_cef_string_utf16_t")
val cefBaseRefCounted = ClassName(cefInteropPackage, "_cef_base_ref_counted_t")
val cefBaseRefCountedKImpl = ClassName("org.jonnyzzz.cef.internal", "KCefRefCountedImpl")

val kotlinString = ClassName("kotlin", "String")

val memberScopeType = ClassName("kotlinx.cinterop", "MemScope")
val arenaType = ClassName("kotlinx.cinterop", "Arena")
val stableRef = ClassName("kotlinx.cinterop", "StableRef")
val cValueType = ClassName("kotlinx.cinterop", "CValue")
val cPointerType = ClassName("kotlinx.cinterop", "CPointer")
val cOpaquePointerVar = ClassName("kotlinx.cinterop", "COpaquePointerVar")

val fnValue = MemberName("kotlinx.cinterop", "value")
val fnPointed = MemberName("kotlinx.cinterop", "pointed")
val fnConvert = MemberName("kotlinx.cinterop", "convert")
val fnStaticCFunction = MemberName("kotlinx.cinterop", "staticCFunction")
val fnMemberAt = MemberName("kotlinx.cinterop", "memberAt")

val fnPosixMemset = MemberName("platform.posix", "memset")
val fnPtr = MemberName("kotlinx.cinterop", "ptr")
val fnReinterpret = MemberName("kotlinx.cinterop", "reinterpret")
val fnAsStableRef = MemberName("kotlinx.cinterop", "asStableRef")
val fnCefStablePrt = MemberName("org.jonnyzzz.cef.internal", "stablePtr")
val fnAlloc = MemberName("kotlinx.cinterop", "alloc")
val fnCValue = MemberName("kotlinx.cinterop", "cValue")
val fnInvoke = MemberName("kotlinx.cinterop", "invoke")

fun ClassifierDescriptor.toClassName(): ClassName = when(val firstParent = parents.first()) {
  is ClassDescriptor -> firstParent.toClassName().nestedClass(name.asString())
  is KonanPackageFragment -> ClassName(firstParent.fqName.asString(), name.asString())
  else -> error("Unsupported type $javaClass: $this")
}

fun TypeProjection.toTypeName() = type.toTypeName()

fun ClassName.asCPointer() = cPointerType.parameterizedBy(this)
fun ClassName.asCValue() = cValueType.parameterizedBy(this)

fun ClassName.asNullableCPointer() = asCPointer().copy(nullable = true)
fun ClassName.asNullableCValue() = asCValue().copy(nullable = true)

fun TypeName.isUnit() = this == UNIT
fun TypeName.isInt() = this == INT
fun TypeName.isLong() = this == LONG
fun TypeName.isString() = this == ClassName("kotlin", "String")
fun TypeName.isUInt() = this == ClassName("kotlin", "UInt")
fun TypeName.isULong() = this == ClassName("kotlin", "ULong")

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

