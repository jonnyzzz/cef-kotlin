import de.undercouch.gradle.tasks.download.Download
import org.jonnyzzz.cef.gradle.div

plugins {
  antlr
  `java-library`
  idea
  id("de.undercouch.download")
}

val packageName = "org.jonnyzzz.cef.gen.c"

val grammarCcommit = "798a1f7f6dc4caa369216572186cc4c66d0da3c5"
val grammarCUrl = "https://github.com/antlr/grammars-v4/raw/$grammarCcommit/c/C.g4"
val grammarCdir = projectDir / "src" / "main" / "antlr" / packageName.replace(".", "/")
val grammarCfile = grammarCdir / (grammarCUrl.split("/").last())

val download = tasks.create<Download>("downloadCgrammar") {
  src(grammarCUrl)
  dest(grammarCfile)
}

dependencies {
  antlr("org.antlr:antlr4:4.7.2")
  api("org.antlr:antlr4-runtime:4.7.2")
}

val generatedSrc = buildDir / "generated-src" / "antlr" / "main"

val generateGrammarSource = tasks.getByName<AntlrTask>("generateGrammarSource"){
  dependsOn(download)
  arguments.addAll(listOf("-package", packageName, "-visitor", "-long-messages"))
//  outputDirectory = generatedSrc / packageName.replace(".", "/") / "c"
}

tasks.getByName("compileJava").dependsOn(generateGrammarSource)

