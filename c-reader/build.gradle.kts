import de.undercouch.gradle.tasks.download.Download
import org.jonnyzzz.cef.gradle.div

plugins {
  antlr
  id("de.undercouch.download")
}

val grammarCcommit = "798a1f7f6dc4caa369216572186cc4c66d0da3c5"
val grammarCUrl = "https://github.com/antlr/grammars-v4/raw/$grammarCcommit/c/C.g4"
val grammarCdir = projectDir / "src" / "main" / "antlr" / "C"
val grammarCfile = grammarCdir / (grammarCUrl.split("/").last())

val download = tasks.create<Download>("downloadCgrammar") {
  src(grammarCUrl)
  dest(grammarCfile)
  overwrite(false)
}

dependencies {
  antlr("org.antlr:antlr4:4.7.2")
}

tasks.getByName<AntlrTask>("generateGrammarSource"){
  dependsOn(download)
  arguments.addAll(listOf("-package", "org.jonnyzzz.cef.gen.c", "-visitor", "-long-messages"))
}

