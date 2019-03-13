import org.jetbrains.kotlin.gradle.plugin.*
import org.jonnyzzz.cef.gradle.div
import org.jonnyzzz.cef.gradle.setupCefConfigurations

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
  implementation("com.squareup:kotlinpoet:1.1.0")
  
  testImplementation("junit:junit:4.12")
}

application {
  mainClassName = "org.jonnyzzz.cef.generator.MainKt"
}

setupCefConfigurations {
  (tasks.run) {
    dependsOn(cef_include)

    val targetDir = project(":cef-mpp").buildDir / "generated-cef"
    val klibFile = project(":cef-mpp").buildDir / "classes/kotlin/macosX64/main/cef-mpp-cinterop-kotlinCefInterop.klib"
    val cefIncludes = includeDir / "include"

    doFirst {
      delete(targetDir)
    }

    args = listOf(
            //TODO: ugly paths!
            klibFile.path,
            cefIncludes.path,
            targetDir.path
    )
  }

}