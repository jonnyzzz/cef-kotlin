package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jonnyzzz.cef.generator.kn.CefKNTypeInfo
import org.jonnyzzz.cef.generator.kn.cefTypeInfo


fun GeneratorParameters.generateTypes2(clazzez: List<ClassDescriptor>) {
  val mappedClasses = clazzez.mapNotNull {
    if (it.kind == ClassKind.ENUM_CLASS) {
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

  mappedClasses.forEach {
    generateType2(it)
  }
}


private fun GeneratorParameters.generateType2(clazz: CefKNTypeInfo): Unit = clazz.run {
  val interfaceFile = FileSpec.builder(
          cefGeneratedPackage,
          sourceInterfaceFileName
  )

  interfaceFile.addType(generateKInterface().build())
  interfaceFile.build().writeTo("api")


  val kotlinToCefFile = FileSpec.builder(
          cefGeneratedPackage,
          sourceKtoCefFileName
  )
          .addImport("org.jonnyzzz.cef", "value", "asString", "copyFrom")

  when {
    rawStruct == cefBaseRefCounted -> { }
    isCefBased -> {
      kotlinToCefFile.addType(generateStructWrapper().build())
      kotlinToCefFile.addFunction(generateWrapKtoCef2(this).build())
      kotlinToCefFile.addFunction(generateWrapKtoCef(this).build())
    }
    else -> {
      kotlinToCefFile.addFunction(generateWrapKtoCefNoBase2(this).build())
      kotlinToCefFile.addFunction(generateWrapKtoCefNoBase(this).build())
    }
  }

  kotlinToCefFile.build().writeTo("k2cef")
}
