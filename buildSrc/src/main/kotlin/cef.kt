package org.jonnyzzz.cef.gradle

import org.gradle.api.PathValidation
import org.gradle.api.Project


val Project.cefHomeMac get() = project(":deps-cef").run {
  //TODO: use dependency from `:deps-cef`, and publish artifact from it instead.
  file(buildDir / "cef_binaries_base" / "cef_mac", PathValidation.DIRECTORY)
}


