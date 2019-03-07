package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.TypeName
import org.jonnyzzz.cef.generator.c.StructFunctionPointer

data class FunctionalPropertyDescriptor(
        val cFieldName : String,
        val funName: String,
        val THIS: DetectedFunctionParam,
        val parameters: List<DetectedFunctionParam>,
        val returnType: TypeName,
        val cefFunctionPointer: StructFunctionPointer?,
        val visibleInInterface : Boolean = true
)
