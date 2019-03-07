package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.TypeName
import org.jonnyzzz.cef.generator.c.StructField

data class FieldPropertyDescriptor(
        val cFieldName : String,
        val propName: String,
        val propType: TypeName,
        val cefMember: StructField?,
        //the C declared type name, before type mapping
        override val originalTypeName: TypeName? = null,
        val visibleInInterface : Boolean = true
) : TypeReplaceableHost<FieldPropertyDescriptor> {
  override val type: TypeName
    get() = propType

  override fun replaceType(newType: TypeName) = copy(originalTypeName = propType, propType = newType)
}
