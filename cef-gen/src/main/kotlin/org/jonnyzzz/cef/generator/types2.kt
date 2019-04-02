package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jonnyzzz.cef.generator.model.CefKNTypeInfo
import org.jonnyzzz.cef.generator.model.DetectedFunctionParam
import org.jonnyzzz.cef.generator.model.KDocumented
import org.jonnyzzz.cef.generator.model.KFunctionalField
import org.jonnyzzz.cef.generator.model.cefTypeInfo


fun GeneratorParameters.generateTypes2(clazzez: List<ClassDescriptor>) {
  val mappedClasses = clazzez.mapNotNull {
    if (it.kind == ClassKind.ENUM_CLASS) {
      //TODO: handle Enum classes (it is necessary to move API as common code)
      return@mapNotNull null
    }

    if (
            it.name.asString().endsWith("_cef_base_scoped_t")
            || it.name.asString().endsWith("_cef_scheme_registrar_t")
            || it.name.asString().endsWith("_cef_string_utf16_t")
            || it.name.asString().endsWith("_cef_string_utf8_t")
            || it.name.asString().endsWith("_cef_string_wide_t")
    ) {
      return@mapNotNull null
    }

    cefTypeInfo(it)
  }

  println("Detected ${mappedClasses.size} classes to generate...")

  val cTokMap = mappedClasses.associate { it.rawStruct to toKNApiTypeInfo(it) }

  fun mapType(type: TypeName): TypeName {
    if (type is ParameterizedTypeName && type.rawType != cPointerType) {
      val struct = type.typeArguments.single()
      val mapped = cTokMap[struct]
      if (mapped != null) {
        return mapped.kInterfaceTypeName.copy(nullable = type.isNullable)
      }
    }

    //TODO: include mapping of Int to Bool where possible (based on documentation analysis)
    //TODO: include mapping of cef_string_* to String (by ref parameters are possible!)
    return type
  }

  cTokMap.values.map {
    replaceTypes(it, ::mapType)
  }.forEach {
    val interfaceFile = FileSpec.builder(
            cefGeneratedPackage,
            it.kInterfaceTypeName.simpleName
    )

    interfaceFile.addType(it.generateKInterface().build())

    interfaceFile.writeTo("api")
  }

  mappedClasses.forEach {
    generateType2(it)
  }
}


private fun GeneratorParameters.generateType2(clazz: CefKNTypeInfo): Unit = clazz.run {
  val kotlinToCefFile = FileSpec.builder(
          cefGeneratedPackage,
          sourceKtoCefFileName
  )
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")

  when {
    rawStruct == cefBaseRefCounted -> {
    }
    cefBaseTypeInfo != null -> {
      toKNRefCountedTypeInfo(this).run {
        kotlinToCefFile.addType(generateStructWrapper().build())
        kotlinToCefFile.addFunction(generateWrapKtoCef2(this).build())
        kotlinToCefFile.addFunction(generateWrapKtoCef(this).build())
      }
    }
    else -> {
      toKNSimpleTypeInfo(this).run {
        kotlinToCefFile.addFunction(generateWrapKtoCefNoBase2(this).build())
        kotlinToCefFile.addFunction(generateWrapKtoCefNoBase(this).build())
      }
    }
  }

  kotlinToCefFile.build().writeTo("k2cef")
}

private fun toKNApiTypeInfo(info: CefKNTypeInfo) = object : KNApiTypeInfo, KDocumented by info {
  override val kInterfaceTypeName = info.kInterfaceTypeName
  override val methods = info.functionProperties.map { f ->
    object : KNApiFunction, KDocumented by f {
      override val memberName = f.funName
      override val returnType = f.returnType
      override val parameters = f.parameters.map { param ->
        object : KNApiFunctionParam {
          override val paramName = param.paramName
          override val paramType = param.paramType
        }
      }
    }
  }
  override val fields = info.fieldProperties.map { p ->
    object : KNApiField, KDocumented by p {
      override val memberName = p.propName
      override val returnType = p.propType
    }
  }
}

private fun toKNRefCountedTypeInfo(info: CefKNTypeInfo) = object : KNRefCountedTypeInfo {
  private val cefBaseTypeInfo = info.cefBaseTypeInfo

  init {
    require(cefBaseTypeInfo != null) { "only CEF Ref counted types, but was $info" }
    require(info.fieldProperties.isEmpty()) { "type $info must not have fields" }
    require(cefBaseTypeInfo.fieldProperties.isEmpty()) { "type $info must not have fields in base" }
  }

  override val rawStruct: ClassName = info.rawStruct
  override val kInterfaceTypeName = info.kInterfaceTypeName
  override val kStructTypeName = info.kStructTypeName
  override val methods = func(info.functionProperties)
  override val refCountMethods = func(info.cefBaseTypeInfo!!.functionProperties)

  private fun param(p: DetectedFunctionParam) = object : KNRefCountedTFunctionParam {
    override val paramName = p.paramName
  }

  private fun func(info: List<KFunctionalField>) = info.map { f ->
    object : KNRefCountedFunction {
      override val cFieldName = f.cFieldName
      override val kFieldName = f.funName

      override val THIS = param(f.THIS)
      override val parameters = f.parameters.map(::param)
    }
  }
}

private fun toKNSimpleTypeInfo(info: CefKNTypeInfo) = object : KNSimpleTypeInfo {
  init {
    require(info.cefBaseTypeInfo == null) { "only simple types are allowed, by was $info" }
    require(info.functionProperties.isEmpty()) { "type $info must not have function properties" }
  }

  override val rawStruct = info.rawStruct
  override val kInterfaceTypeName = info.kInterfaceTypeName
  override val fields = info.fieldProperties.map { f ->
    object : KNSimpleField {
      override val memberName = f.propName
      override val returnType = f.propType
    }
  }
}