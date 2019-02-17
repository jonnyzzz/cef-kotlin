package org.jonnyzzz.cef.generator

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities

/*
* This code is mostly from Kotlin/Native sources
* licensed as Apache 2.0 by JetBrains
* https://github.com/jetbrains/kotlin-native
* */


val DeclarationDescriptorWithVisibility.isPublicOrProtected: Boolean
  get() = visibility == Visibilities.PUBLIC || visibility == Visibilities.PROTECTED

val CallableMemberDescriptor.isFakeOverride: Boolean
  get() = kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE

val DeclarationDescriptor.shouldBePrinted: Boolean
  get() = this is ClassifierDescriptorWithTypeParameters && isPublicOrProtected
          || this is CallableMemberDescriptor && isPublicOrProtected && !isFakeOverride

