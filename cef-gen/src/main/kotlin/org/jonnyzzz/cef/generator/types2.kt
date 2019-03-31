package org.jonnyzzz.cef.generator

import com.squareup.kotlinpoet.FileSpec
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jonnyzzz.cef.generator.kn.cefTypeInfo


fun GeneratorParameters.generateTypes2(clazzez: List<ClassDescriptor>) {
  clazzez.forEach {

    if (it.name.asString() in setOf("_cef_browser_process_handler_t", "_cef_base_ref_counted_t", "_cef_app_t", "_cef_before_download_callback_t", "_cef_settings_t", "_cef_client_t", "_cef_window_info_t", "_cef_browser_settings_t")) {
      generateType2(it)
    }

//    if (it.name.asString() != "sched_param" && it.getSuperClassNotAny()?.classId == ClassId.fromString("kotlinx/cinterop/CStructVar")) {
//      generateType(it, copyFromTypes)
//    }
  }
}


private fun GeneratorParameters.generateType2(clazz: ClassDescriptor): Unit = cefTypeInfo(clazz).run {
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
