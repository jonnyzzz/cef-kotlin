package org.jonnyzzz.cef.generator

/*
fun GeneratorParameters.generateImplBase(info: CefKNTypeInfo) = info.run {
  TypeSpec.classBuilder(kImplBaseTypeName).apply {
    addModifiers(KModifier.ABSTRACT)
    addSuperinterface(kInterfaceTypeName)
    primaryConstructor(
            FunSpec.constructorBuilder()
                    .addParameter("scope", ClassName("kotlinx.cinterop", "MemScope"))
                    .build()
    )

    info.cefBaseTypeInfo?.let { baseInfo ->
      addSuperinterface(baseInfo.kInterfaceTypeName, CodeBlock.of("%T()", cefBaseRefCountedKImpl))
    }

    addProperty(PropertySpec
            .builder("ptr", rawStruct.asCPointer())
            .getter(FunSpec
                    .getterBuilder()
                    .addStatement("return cValue.%M<%T>().%M", fnReinterpret, rawStruct, MemberName("kotlinx.cinterop", "ptr"))
                    .build()
            ).build()
    )

    addProperty(PropertySpec
            .builder("stableRef", ParameterizedTypeName.run { stableRef.parameterizedBy(kImplBaseTypeName) })
            .addModifiers(KModifier.PRIVATE)
            .initializer("scope.%M(this)", MemberName("org.jonnyzzz.cef.internal", "stablePtr"))
            .build()
    )

    addProperty(PropertySpec
            .builder("cValue", kStructTypeName)
            .addModifiers(KModifier.PRIVATE)
            //TODO: .initializer(generateCValueInitBlock(info).build())
            .build()
    )


    info.fieldProperties.forEach { p ->
      val spec = PropertySpec
              .builder(p.propName, p.propType, KModifier.OVERRIDE).mutable(true)
              .getter(FunSpec
                      .getterBuilder()
                      .addStatement("return cValue." + p.fromCefToKotlin("cef.${p.cFieldName}"))
                      .build()
              )
              .setter(FunSpec
                      .setterBuilder()
                      .addParameter("value", p.propType)
                      .apply {
                        //TODO: hide inside KPropertyField!
                        if (p.originalTypeName ?: p.propType in copyFromTypeNames) {
                          addStatement("cValue.cef.${p.cFieldName}.copyFrom(value)")
                        } else {
                          addStatement("cValue.cef.${p.cFieldName} = value")
                        }
                      }
                      .build()
              )


      addProperty(spec.build())
    }
  }
}
*/
