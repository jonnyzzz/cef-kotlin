plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish")
}

dependencies {
  compileOnly(kotlin("gradle-plugin", version = "1.3.21"))
}


kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

