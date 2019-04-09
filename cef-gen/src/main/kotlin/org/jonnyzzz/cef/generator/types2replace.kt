package org.jonnyzzz.cef.generator

interface TypeReplaced<T> {
  val origin : T
}

fun replaceTypes(info: KNApiTypeInfo, replace: CefTypeSubstitution) : KNApiTypeInfo = object : KNApiTypeInfo by info, TypeReplaced<KNApiTypeInfo> {
  override val origin = info
  override val kMethods = info.kMethods.map { replaceTypes(it, replace) }
  override val kFields = info.kFields.map { replaceTypes(it, replace) }
}

fun replaceTypes(info: KNApiField, replace: CefTypeSubstitution) : KNApiField = object : KNApiField by info, TypeReplaced<KNApiField> {
  override val origin = info
  override val kReturnType = replace.mapTypeFromCefToK(info)
}

fun replaceTypes(info: KNApiFunctionParam, replace: CefTypeSubstitution) : KNApiFunctionParam = object : KNApiFunctionParam by info, TypeReplaced<KNApiFunctionParam> {
  override val origin = info
  override val kParamType = replace.mapTypeFromCefToK(info)
}

fun replaceTypes(info: KNApiFunction, replace: CefTypeSubstitution) : KNApiFunction = object : KNApiFunction by info, TypeReplaced<KNApiFunction> {
  override val origin = info
  override val parameters = info.parameters.map { replaceTypes(it, replace) }
  override val kReturnType = replace.mapTypeFromCefToK(info)
}

