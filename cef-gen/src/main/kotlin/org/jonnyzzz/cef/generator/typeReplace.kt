package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.TypeName


interface TypeReplaceableHost<T> {
  val type : TypeName
  val originalTypeName: TypeName?
  fun replaceType(newType: TypeName) : T
}


fun <T : TypeReplaceableHost<T>> T.replaceToKotlinTypes() : T {
  return when (type) {
    cefString16 -> replaceType(kotlinString)
    cefString16.asNullableCPointer() -> replaceType(kotlinString.copy(nullable = true))
    else -> this
  }
}


fun <T: TypeReplaceableHost<T>> T.fromCefToKotlin(paramName: String) : String = when (originalTypeName) {
  null -> paramName
  cefString16 -> "$paramName.asString()"
  cefString16.asNullableCPointer() -> "$paramName?.asString()"
  else -> TODO("Not supported for $this")
}
