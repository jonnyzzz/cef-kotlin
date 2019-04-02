package org.jonnyzzz.cef.generator.model

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

interface KDocumented {
  val docComment: String?
}
private fun String.sanitize() = replace("%", "%%")
        /// javadoc may contain /* or */, but Kotlin does assume is as comments,
        /// kotlinpoet does not escape it too
        .replace("/*", "/\u200b*")
        .replace("*/", "*\u200b/")

fun TypeSpec.Builder.addKdoc(d: KDocumented) = apply {
  try {
    d.docComment?.let {
      addKdoc(it.sanitize())
    }
  } catch (t: Throwable) {
    throw Error("Failed to process $d. $t", t)
  }
}

fun PropertySpec.Builder.addKdoc(d: KDocumented) = apply {
  try {
    d.docComment?.let {
      addKdoc(it.sanitize())
    }
  } catch (t: Throwable) {
    throw Error("Failed to process $d. $t", t)
  }
}

fun FunSpec.Builder.addKdoc(d: KDocumented) = apply {
  try {
    d.docComment?.let {
      addKdoc(it.sanitize())
    }
  } catch (t: Throwable) {
    throw Error("Failed to process $d. $t", t)
  }
}

