package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.TypeName

data class DetectedFunctionParam(
        val paramName: String,
        val paramType: TypeName,
        //the C declared type name, before type mapping
        override val originalTypeName: TypeName? = null
) : TypeReplaceableHost<DetectedFunctionParam> {
  override val type: TypeName
    get() = paramType

  override fun replaceType(newType: TypeName) = copy(originalTypeName = paramType, paramType = newType)
}
