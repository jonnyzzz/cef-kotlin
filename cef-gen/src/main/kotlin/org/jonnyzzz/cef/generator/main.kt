package org.jonnyzzz.cef.generator

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jonnyzzz.cef.generator.c.loadCefDeclarations
import org.jonnyzzz.cef.generator.model.cefTypeInfo
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
  println(" <too> cef.klib <cef_includes> <outputDir>")

  val klibPath = args.getOrNull(0)?.let { File(it).absoluteFile } ?: error("Failed to find .klib")
  val cefIncludesDir = args.getOrNull(1)?.let { File(it).absoluteFile } ?: error("Failed to find CEF includes dir")
  val outputDir = args.getOrNull(2)?.let { File(it).absoluteFile } ?: error("Failed to find target directory")

  //TODO: resolve correct K/N library path (aka internal version)!
  val stdlibPath = File(System.getProperty("user.home")) / ".konan" / "kotlin-native-macos-1.2-eap-9117" / "klib" / "common" / "stdlib"

  val stdlib = createKonanLibrary(
          libraryFile = KFile(stdlibPath.toPath()),
          currentAbiVersion = 8 /*TODO: HACK!*/,
          target = null,
          isDefault = true
  )

  val library = createKonanLibrary(
          libraryFile = KFile(klibPath.toPath()),
          currentAbiVersion = 8 /*TODO: HACK!*/,
          target = KonanTarget.MACOS_X64 /*TODO: cross platform*/
  )

  println("library - ${library.abiVersion}")
  println("library - ${library.libraryFile}")
  println("library - ${library.libraryName}")
  println("library - ${library.linkerOpts}")

  val storageManager = LockBasedStorageManager("debug-text")
  val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
  val module = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager)
  val stdlibModule = DefaultDeserializedDescriptorFactory.createDescriptor(stdlib, versionSpec, storageManager, module.builtIns)

  stdlibModule.setDependencies(listOf(stdlibModule))
  module.setDependencies(listOf(module, stdlibModule))

  val generatorParams = GeneratorParameters(cefIncludesDir, outputDir)
  generatorParams.visitModule(module)
}


private fun GeneratorParameters.visitModule(module: ModuleDescriptor) {
  println(module)

  val descriptors = sequence {
    val queue = ArrayDeque(listOf(FqName.ROOT))
    val visited = mutableSetOf<FqName>()
    while (queue.isNotEmpty()) {
      val next = queue.removeFirst()
      if (!visited.add(next)) continue

      queue.addAll(module.getSubPackagesOf(next) { true })
      yield(module.getPackage(next))
    }
  }.filter { it.module === module && it.fqName.startsWith(Name.identifier("org.jonnyzzz.cef.interop")) }
  .toList().flatMap {
    it.fragments
  }.flatMap {
    it.getMemberScope().getContributedDescriptors().filter { it.shouldBePrinted }
  }

  println("------------\n")

  resolveCefCDeclarations()
  resolveCefBaseRefCounted(descriptors.filterIsInstance<ClassDescriptor>())
  collectAllEnums(descriptors.filterIsInstance<ClassDescriptor>())
  generateCopyFunctions(descriptors.filterIsInstance<ClassDescriptor>())
  generateValFunctions(descriptors.filterIsInstance<PropertyDescriptor>())
  generateTypes2(descriptors.filterIsInstance<ClassDescriptor>())
}

private fun GeneratorParameters.resolveCefCDeclarations() {
  cefDeclarations = loadCefDeclarations(cefIncludesDir)
}

private fun GeneratorParameters.resolveCefBaseRefCounted(classes: List<ClassDescriptor>) {
  cefBaseClassDescriptor = classes.first { it.toClassName() == cefBaseRefCounted }
  cefBaseClassDescriptorInfo = cefTypeInfo(cefBaseClassDescriptor)
}

private fun GeneratorParameters.collectAllEnums(classes: List<ClassDescriptor>) {
  enumTypes = classes.filter { clazz ->
    clazz.getSuperInterfaces().any { it.fqNameSafe.asString() == "kotlinx.cinterop.CEnum" }
  }.map { it.toClassName() }.toSet()
}
