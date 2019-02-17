package org.jonnyzzz.cef.generator

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.utils.KonanFactories.DefaultDeserializedDescriptorFactory
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


private fun mainImpl(args: Array<String>) {
  println("Kotlin CEF API generator.")
  println("  the part similar to https://bitbucket.org/chromiumembedded/cef/src/master/tools/translator.README.txt")
  println()
  println("usage:")
  println(" <too> cef.klib")

  val klibPath = args.getOrNull(0)?.let { File(it).absoluteFile } ?: error("Failed to find .klib")

  val library = resolverByName(emptyList(), skipCurrentDir = true).resolve(klibPath.path)
  val storageManager = LockBasedStorageManager()
  val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
  val module: ModuleDescriptorImpl = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager)


  run {
    val defaultModules = mutableListOf<ModuleDescriptorImpl>()
    val resolver = resolverByName(
            emptyList(),
            distributionKlib = Distribution().klib,
            skipCurrentDir = true)
    resolver.defaultLinks(noStdLib = false, noDefaultLibs = true)
            .mapTo(defaultModules) {
              DefaultDeserializedDescriptorFactory.createDescriptor(
                      it, versionSpec, storageManager, module.builtIns)
            }

    (defaultModules + module).let { allModules ->
      allModules.forEach { it.setDependencies(allModules) }
    }
  }


  println(module)

  println(module.allDependencyModules)

  val packages = sequence {
    val queue = ArrayDeque(listOf(FqName.ROOT))
    val visited = mutableSetOf<FqName>()
    while(queue.isNotEmpty()) {
      val next = queue.removeFirst()
      if (!visited.add(next)) continue

      queue.addAll(module.getSubPackagesOf(next) { true })
      yield(module.getPackage(next))
    }
  }

  for (pkg in packages) {
    println("package: $pkg")
  }

}

