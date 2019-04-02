package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jonnyzzz.cef.generator.model.CefKNTypeInfo
import org.jonnyzzz.cef.generator.model.addKdoc

fun CefKNTypeInfo.generateKInterface(): TypeSpec.Builder {
  val kInterface = TypeSpec.classBuilder(kInterfaceTypeName).addKdoc(this)

  if (functionProperties.isNotEmpty()) {
    kInterface.addModifiers(KModifier.ABSTRACT)
  }

  if (fieldProperties.isNotEmpty()) {
    val constr = FunSpec.constructorBuilder()
    fieldProperties.forEach { p ->
      val parameterSpec = ParameterSpec.builder(p.propName, p.propType)

      //default implementation for nullable types
      when {
        p.propType.isNullable -> {
          parameterSpec.defaultValue("null")
        }
        p.propType.isInt() -> {
          parameterSpec.defaultValue("0")
        }
        p.propType.isUInt() -> {
          parameterSpec.defaultValue("0U")
        }
        p.propType.isString() -> {
          parameterSpec.defaultValue("%S", "")
        }
      }

      constr.addParameter(parameterSpec.build())
    }

    kInterface.primaryConstructor(constr.build())

    fieldProperties.forEach { p ->
      val pSpec = PropertySpec.builder(p.propName, p.propType).mutable(true).addKdoc(p).initializer(p.propName)
      kInterface.addProperty(pSpec.build())
    }
  }

  functionProperties.forEach { p ->
    val fSpec = FunSpec.builder(p.funName).addKdoc(p)

    p.parameters.forEach {
      fSpec.addParameter(it.paramName, it.paramType)
    }

    fSpec.returns(p.returnType)

    //default implementation for nullable types
    when {
      p.returnType.isNullable -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return null")
      }
      p.returnType.isUnit() -> {
        fSpec.addModifiers(KModifier.OPEN)
      }
      p.returnType.isInt() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return 0")
      }
      p.returnType.isString() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return %S", "")
      }
      p.returnType.isUInt() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return 0U")
      }
      else -> fSpec.addModifiers(KModifier.ABSTRACT)
    }

    kInterface.addFunction(fSpec.build())
  }

  return kInterface
}

