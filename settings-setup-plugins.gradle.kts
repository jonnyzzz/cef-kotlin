import org.gradle.internal.impldep.org.apache.ivy.core.IvyPatternHelper.substitute

pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("org.jonnyzzz.cef")) {
        useModule("org.jonnyzzz.cef.plugins:kotlin-cef:LOCAL_BUILD")
      }
    }
  }
}

val pluginsProjectDir = sequence {
  val pluginsProjectDirName = "gradle-plugins"
  yield(File(rootProject.projectDir, pluginsProjectDirName))
  yield(File(rootProject.projectDir, "../$pluginsProjectDirName"))
}.first { it.isDirectory }

includeBuild(pluginsProjectDir) {
  dependencySubstitution {
    substitute(module("org.jonnyzzz.cef.plugins:kotlin-cef")).with(project(":kotlin-cef"))
  }
}
