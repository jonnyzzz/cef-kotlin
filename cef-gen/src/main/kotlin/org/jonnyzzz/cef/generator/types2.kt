package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jonnyzzz.cef.generator.model.CefApiModel
import org.jonnyzzz.cef.generator.model.buildCefImplModel
import org.jonnyzzz.cef.generator.model.cefTypeInfo


fun GeneratorParameters.resolveCefTypes2(clazzez: List<ClassDescriptor>) {
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

  cefTypeSubstitution = CefTypeSubstitution(mappedClasses - cefBaseClassDescriptorInfo/*org.jonnyzzz.cef.generated.KCefV8ValueWrapper.getUserData*/)
  cefApiModel = CefApiModel(mappedClasses, cefTypeSubstitution)
  cefImplModel = buildCefImplModel(cefApiModel)
  cefTypeMapperGenerator = CefTypeMapperGenerator(cefImplModel.simpleTypes, cefImplModel.refCounted)
}

fun GeneratorParameters.generateTypes2() {
  cefApiModel.apiTypes.forEach {
    val interfaceFile = FileSpec.builder(
            cefGeneratedPackage,
            it.kInterfaceTypeName.simpleName
    )

    interfaceFile.addType(it.generateKInterface().build())

    interfaceFile.writeTo("api")
  }

  cefImplModel.refCounted.forEach {
    val kotlinToCefFile = FileSpec.builder(
            cefGeneratedPackage,
            it.kInterfaceTypeName.simpleName + "Bridge"
    ).addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
    kotlinToCefFile.addType(it.generateStructWrapper().build())
    generateWrapCefToK(kotlinToCefFile, cefTypeMapperGenerator, it)
    kotlinToCefFile.addFunction(generateWrapKtoCef(cefTypeMapperGenerator, it).build())
    kotlinToCefFile.writeTo("k2cef")
  }

  cefImplModel.simpleTypes.forEach {
    val kotlinToCefFile = FileSpec.builder(
            cefGeneratedPackage,
            it.kInterfaceTypeName.simpleName + "Bridge"
    ).addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")
    generateWrapKtoCefNoBase(kotlinToCefFile, it, cefTypeMapperGenerator)
    generateWrapCefToKNoBase(kotlinToCefFile, it, cefTypeMapperGenerator)
    kotlinToCefFile.writeTo("k2cef")
  }
}
