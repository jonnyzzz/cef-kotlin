package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import org.jonnyzzz.cef.generator.model.KDocumented
import org.jonnyzzz.cef.generator.model.addKdoc

interface KNApiMember : KDocumented {
  val memberName: String
  val returnType: TypeName
}

interface KNApiField : KNApiMember

interface KNApiFunctionParam {
  val paramName: String
  val paramType: TypeName
}

interface KNApiFunction : KNApiMember {
  val parameters: List<KNApiFunctionParam>
}

interface KNApiTypeInfo : KDocumented {
  val kInterfaceTypeName: ClassName

  val methods: List<KNApiFunction>
  val fields: List<KNApiField>
}


fun KNApiTypeInfo.generateKInterface(): TypeSpec.Builder {
  val kInterface = TypeSpec.classBuilder(kInterfaceTypeName).addKdoc(this)

  if (methods.isNotEmpty()) {
    kInterface.addModifiers(KModifier.ABSTRACT)
  }

  if (fields.isNotEmpty()) {
    val constr = FunSpec.constructorBuilder()
    fields.forEach { p ->
      val parameterSpec = ParameterSpec.builder(p.memberName, p.returnType)

      //default implementation for nullable types
      when {
        p.returnType.isNullable -> {
          parameterSpec.defaultValue("null")
        }
        p.returnType.isInt() -> {
          parameterSpec.defaultValue("0")
        }
        p.returnType.isUInt() -> {
          parameterSpec.defaultValue("0U")
        }
        p.returnType.isString() -> {
          parameterSpec.defaultValue("%S", "")
        }
      }

      constr.addParameter(parameterSpec.build())
    }

    kInterface.primaryConstructor(constr.build())

    fields.forEach { p ->
      val pSpec = PropertySpec.builder(p.memberName, p.returnType).mutable(true).addKdoc(p).initializer(p.memberName)
      kInterface.addProperty(pSpec.build())
    }
  }

  methods.forEach { p ->
    val fSpec = FunSpec.builder(p.memberName).addKdoc(p)

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

