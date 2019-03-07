package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName

data class DetectedFunction(
        val funcDeclaration: FunSpec.Builder,
        val params: List<DetectedFunctionParam>,
        val returnType: TypeName
)
