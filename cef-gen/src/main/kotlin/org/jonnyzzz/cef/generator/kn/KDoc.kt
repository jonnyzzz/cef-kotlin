package org.jonnyzzz.cef.generator.kn

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

interface KDocumented {
  val docComment: String?
}

fun TypeSpec.Builder.addKdoc(d: KDocumented) = apply {
  d.docComment?.let {
    addKdoc(it)
  }
}


fun PropertySpec.Builder.addKdoc(d: KDocumented) = apply {
  d.docComment?.let {
    addKdoc(it)
  }
}

fun FunSpec.Builder.addKdoc(d: KDocumented) = apply {
  d.docComment?.let {
    addKdoc(it)
  }
}

