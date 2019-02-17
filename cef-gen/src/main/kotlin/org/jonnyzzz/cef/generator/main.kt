package org.jonnyzzz.cef.generator

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.konan.library.SearchPathResolver
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedModuleDescriptorFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File

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

  val library = createKonanLibrary(
          libraryFile = KFile(klibPath.toPath()),
          currentAbiVersion = 5 /*TODO: HACK!*/
  )

  val storageManager = LockBasedStorageManager()
  val versionSpec = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
  val module = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager)

  println(module)


}

