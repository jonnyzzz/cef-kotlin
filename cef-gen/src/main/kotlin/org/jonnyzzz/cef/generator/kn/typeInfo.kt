package org.jonnyzzz.cef.generator.kn

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jonnyzzz.cef.generator.cefBaseRefCounted
import org.jonnyzzz.cef.generator.shouldBePrinted
import org.jonnyzzz.cef.generator.toTypeName


fun ClassDescriptor.allMeaningfulProperties() =
        getMemberScope(TypeSubstitution.EMPTY).getContributedDescriptors()
                .filter { it.shouldBePrinted }
                .filterIsInstance<PropertyDescriptor>()
                .filter { it.name.asString() !in setOf("size", "base") }

