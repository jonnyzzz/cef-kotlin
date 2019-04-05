package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jonnyzzz.cef.generator.model.addKdoc


fun KNApiTypeInfo.generateKInterface(): TypeSpec.Builder {
  val kInterface = TypeSpec.classBuilder(kInterfaceTypeName).addKdoc(this)

  if (kMethods.isNotEmpty()) {
    kInterface.addModifiers(KModifier.ABSTRACT)
  }

  if (kFields.isNotEmpty()) {
    val constr = FunSpec.constructorBuilder()
    kFields.forEach { p ->
      val parameterSpec = ParameterSpec.builder(p.kFieldName, p.kReturnType)

      //default implementation for nullable types
      when {
        p.kReturnType.isNullable -> {
          parameterSpec.defaultValue("null")
        }
        p.kReturnType.isInt() -> {
          parameterSpec.defaultValue("0")
        }
        p.kReturnType.isLong() -> {
          parameterSpec.defaultValue("0L")
        }
        p.kReturnType.isUInt() -> {
          parameterSpec.defaultValue("0U")
        }
        p.kReturnType.isULong() -> {
          parameterSpec.defaultValue("0UL")
        }
        p.kReturnType.isString() -> {
          parameterSpec.defaultValue("%S", "")
        }
      }

      constr.addParameter(parameterSpec.build())
    }

    kInterface.primaryConstructor(constr.build())

    kFields.forEach { p ->
      val pSpec = PropertySpec.builder(p.kFieldName, p.kReturnType).mutable(true).addKdoc(p).initializer(p.kFieldName)
      kInterface.addProperty(pSpec.build())
    }
  }

  kMethods.forEach { p ->
    val fSpec = FunSpec.builder(p.kFieldName).addKdoc(p)

    p.parameters.forEach {
      fSpec.addParameter(it.kParamName, it.kParamType)
    }

    fSpec.returns(p.kReturnType)

    //default implementation for nullable types
    when {
      p.kReturnType.isNullable -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return null")
      }
      p.kReturnType.isUnit() -> {
        fSpec.addModifiers(KModifier.OPEN)
      }
      p.kReturnType.isInt() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return 0")
      }
      p.kReturnType.isLong() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return 0L")
      }
      p.kReturnType.isULong() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return 0UL")
      }
      p.kReturnType.isString() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return %S", "")
      }
      p.kReturnType.isUInt() -> {
        fSpec.addModifiers(KModifier.OPEN)
        fSpec.addStatement("return 0U")
      }
      else -> fSpec.addModifiers(KModifier.ABSTRACT)
    }

    kInterface.addFunction(fSpec.build())
  }

  return kInterface
}

