package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jonnyzzz.cef.generator.kn.CefKNTypeInfo
import org.jonnyzzz.cef.generator.kn.addKdoc

fun CefKNTypeInfo.generateKInterface(): TypeSpec.Builder {
  val kInterface = TypeSpec.interfaceBuilder(kInterfaceTypeName).addKdoc(this)

  //do we really need that base interface explicitly?
  /*
  if (clazz.isCefBased) {
    type.addSuperinterface(cefTypeInfo(cefBaseRefCounted).kInterfaceTypeName)
  }*/

  functionProperties.filter { it.visibleInInterface }.forEach { p ->
    val fSpec = FunSpec.builder(p.funName).addKdoc(p)

    p.parameters.forEach {
      fSpec.addParameter(it.paramName, it.paramType)
    }

    fSpec.returns(p.returnType)

    //default implementation for nullable types
    if (p.returnType.isNullable) {
      fSpec.addStatement("return null")
    } else {
      fSpec.addModifiers(KModifier.ABSTRACT)
    }

    kInterface.addFunction(fSpec.build())
  }

  fieldProperties.filter { it.visibleInInterface }.forEach { p ->
    val pSpec = PropertySpec.builder(p.propName, p.propType).mutable(true).addKdoc(p)
    kInterface.addProperty(pSpec.build())
  }
  return kInterface
}

