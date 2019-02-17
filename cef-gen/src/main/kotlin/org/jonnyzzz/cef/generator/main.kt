package org.jonnyzzz.cef.generator

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File
import java.util.*

typealias KFile = org.jetbrains.kotlin.konan.file.File

fun main(args: Array<String>) {
  try {
    mainImpl(args)
    System.exit(0)
  } catch (t: Throwable) {
    println("Unexpected exception: $t")
    t.printStackTrace()
    System.exit(1)
  }
}

operator fun File.div(s: String) = File(this, s)

private fun mainImpl(args: Array<String>) {
  println("Kotlin CEF API generator.")
  println("  the part similar to https://bitbucket.org/chromiumembedded/cef/src/master/tools/translator.README.txt")
  println()
  println("usage:")
  println(" <too> cef.klib")

  val klibPath = args.getOrNull(0)?.let { File(it).absoluteFile } ?: error("Failed to find .klib")
  val stdlibPath = File(System.getProperty("user.home")) / ".konan" / "kotlin-native-macos-1.1.2" / "klib" / "common" / "stdlib"

  val stdlib = createKonanLibrary(
          libraryFile = KFile(stdlibPath.toPath()),
          currentAbiVersion = 5 /*TODO: HACK!*/,
          target = null,
          isDefault = true
  )

  val library = createKonanLibrary(
          libraryFile = KFile(klibPath.toPath()),
          currentAbiVersion = 5 /*TODO: HACK!*/,
          target = KonanTarget.MACOS_X64 /*TODO: cross platform*/
  )

  println("library - ${library.abiVersion}")
  println("library - ${library.libraryFile}")
  println("library - ${library.libraryName}")

  val storageManager = LockBasedStorageManager()
  val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
  val module = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager)
  val stdlibModule = DefaultDeserializedDescriptorFactory.createDescriptor(stdlib, versionSpec, storageManager, module.builtIns)

  stdlibModule.setDependencies(listOf(stdlibModule))
  module.setDependencies(listOf(module, stdlibModule))

  visitModule(module)
}


private fun visitModule(module : ModuleDescriptor) {
  println(module)

  sequence {
    val queue = ArrayDeque(listOf(FqName.ROOT))
    val visited = mutableSetOf<FqName>()
    while(queue.isNotEmpty()) {
      val next = queue.removeFirst()
      if (!visited.add(next)) continue

      queue.addAll(module.getSubPackagesOf(next) { true })
      yield(module.getPackage(next))
    }
  }.toList().flatMap {
    println("package: $it")
    it.fragments
  }.flatMap {
    println("fragment: $it")
    it.getMemberScope().getContributedDescriptors().filter { it.shouldBePrinted }
  }.forEach {
    println("DeclarationScope: $it")}
}
