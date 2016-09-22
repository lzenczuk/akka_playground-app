import sbt._
import Keys._
import sbtassembly.AssemblyKeys.assembly

object Commons {
  val appVersion = "0.1"

  val settings: Seq[Def.Setting[_]] = Seq(
    version := appVersion,
    scalaVersion := "2.11.8",
    resolvers += Opts.resolver.mavenLocalFile,
    test in assembly := {}
  )
}