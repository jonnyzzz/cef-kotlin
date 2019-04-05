package org.jonnyzzz.cef.generator

import org.jonnyzzz.cef.generator.model.CefKNTypeInfo


class CefApiModel(mappedClasses: List<CefKNTypeInfo>,
                  private val substitution: CefTypeSubstitution) {

  private val mappedTypes = mappedClasses.map { type ->
    type to replaceTypes(toKNApiTypeInfo(type), substitution::mapTypeFromCefToK)
  }

  val apiTypes = mappedTypes.asSequence().map { it.second }.asSequence()
  val raw = mappedTypes.asSequence()
}



