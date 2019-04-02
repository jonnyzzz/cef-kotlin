package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.TypeName

interface TypeReplaced<T> {
  val origin : T
}

fun replaceTypes(info: KNApiTypeInfo, replace: (TypeName) -> TypeName) : KNApiTypeInfo = object : KNApiTypeInfo by info, TypeReplaced<KNApiTypeInfo> {
  override val origin = info
  override val methods = info.methods.map { replaceTypes(it, replace) }
  override val fields = info.fields.map { replaceTypes(it, replace) }
}

fun replaceTypes(info: KNApiField, replace: (TypeName) -> TypeName) : KNApiField= object : KNApiField by info, TypeReplaced<KNApiField> {
  override val origin = info
  override val returnType = replace(info.returnType)
}

fun replaceTypes(info: KNApiFunctionParam, replace: (TypeName) -> TypeName) : KNApiFunctionParam= object : KNApiFunctionParam by info, TypeReplaced<KNApiFunctionParam> {
  override val origin = info
  override val paramType = replace(info.paramType)
}

fun replaceTypes(info: KNApiFunction, replace: (TypeName) -> TypeName) : KNApiFunction= object : KNApiFunction by info, TypeReplaced<KNApiFunction> {
  override val origin = info
  override val parameters = info.parameters.map { replaceTypes(it, replace) }
  override val returnType = replace(info.returnType)
}

