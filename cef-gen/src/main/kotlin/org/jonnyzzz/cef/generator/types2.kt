package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
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

  val substitution = CefTypeSubstitution(mappedClasses)
  val cToMappedApi = CefApiModel(mappedClasses, substitution)

  cToMappedApi.apiTypes.forEach {
    val interfaceFile = FileSpec.builder(
            cefGeneratedPackage,
            it.kInterfaceTypeName.simpleName
    )

    interfaceFile.addType(it.generateKInterface().build())

    interfaceFile.writeTo("api")
  }


  val refCounted = cToMappedApi.raw.mapNotNull { (raw, api) ->
    if (raw.rawStruct == cefBaseRefCounted) return@mapNotNull null
    if (raw.cefBaseTypeInfo == null) return@mapNotNull null

    toKNRefCountedTypeInfo(api, raw)
  }

  val simpleTypes = cToMappedApi.raw.mapNotNull { (raw, api) ->
    if (raw.rawStruct == cefBaseRefCounted) return@mapNotNull null
    if (raw.cefBaseTypeInfo != null) return@mapNotNull null

    toKNSimpleTypeInfo(api, raw)
  }

  val mapper = CefTypeMapperGenerator(simpleTypes, refCounted)


  refCounted.forEach {
    val kotlinToCefFile = FileSpec.builder(
            cefGeneratedPackage,
            it.kInterfaceTypeName.simpleName + "Bridge"
    ).addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
    kotlinToCefFile.addType(it.generateStructWrapper().build())
    generateWrapCefToK(kotlinToCefFile, mapper, it)
    kotlinToCefFile.addFunction(generateWrapKtoCef(mapper, it).build())
    kotlinToCefFile.writeTo("k2cef")
  }

  simpleTypes.forEach {
    val kotlinToCefFile = FileSpec.builder(
            cefGeneratedPackage,
            it.kInterfaceTypeName.simpleName + "Bridge"
    ).addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
    kotlinToCefFile.addFunction(generateWrapKtoCefNoBase(it).build())
    kotlinToCefFile.writeTo("k2cef")
  }
}
