package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.TypeName

interface TypeReplaced<T> {
  val origin : T
}

fun replaceTypes(info: KNApiTypeInfo, replace: (TypeName) -> TypeName) : KNApiTypeInfo = object : KNApiTypeInfo by info, TypeReplaced<KNApiTypeInfo> {
  override val origin = info
  override val kMethods = info.kMethods.map { replaceTypes(it, replace) }
  override val kFields = info.kFields.map { replaceTypes(it, replace) }
}

fun replaceTypes(info: KNApiField, replace: (TypeName) -> TypeName) : KNApiField = object : KNApiField by info, TypeReplaced<KNApiField> {
  override val origin = info
  override val kReturnType = replace(info.kReturnType)
}

fun replaceTypes(info: KNApiFunctionParam, replace: (TypeName) -> TypeName) : KNApiFunctionParam = object : KNApiFunctionParam by info, TypeReplaced<KNApiFunctionParam> {
  override val origin = info
  override val kParamType = replace(info.kParamType)
}

fun replaceTypes(info: KNApiFunction, replace: (TypeName) -> TypeName) : KNApiFunction = object : KNApiFunction by info, TypeReplaced<KNApiFunction> {
  override val origin = info
  override val parameters = info.parameters.map { replaceTypes(it, replace) }
  override val kReturnType = replace(info.kReturnType)
}

