package org.jonnyzzz.cef.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultCInteropSettings
import org.gradle.api.Task
import java.io.*

operator fun File.div(s: String) = File(this, s)


fun DefaultCInteropSettings.setupInteropProcessingTask(project: Project, action: Task.() -> Unit) = project.run {
  tasks.all {
    if (name == interopProcessingTaskName) {
      action()
    }
  }
}
