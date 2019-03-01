import org.jetbrains.kotlin.gradle.plugin.*
import org.jonnyzzz.cef.gradle.div

plugins {
  kotlin("jvm")
  application
}

repositories {
  mavenCentral()
}

val kotlinVersion by lazy {
  //https://youtrack.jetbrains.com/issue/KT-19788
  plugins.withType<KotlinBasePluginWrapper>().map { it.kotlinPluginVersion }.distinct().single()
}

configurations.forEach { config ->
  config.resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
      useVersion(kotlinVersion)
    }
  }
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-native-library-reader:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
  
  implementation("com.squareup:kotlinpoet:1.0.1")
}

configure<ApplicationPluginConvention> {
  mainClassName = "org.jonnyzzz.cef.generator.MainKt"
}


tasks.getByName<JavaExec>("run") {
  val targetDir = project(":cef-mpp").buildDir / "generated-cef"
  val klibFile = project(":cef-mpp").buildDir / "classes/kotlin/macosX64/main/cef-mpp-cinterop-kotlinCefInterop.klib"

  doFirst {
    delete(targetDir)
  }
  1
  args = listOf(
          //TODO: ugly paths!
          klibFile.path,
          targetDir.path
  )
}
