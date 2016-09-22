import sbt._
import Keys._

object Dependencies {
  val akkaVersion = "2.4.10"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  val akkaHttp = "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion

  val akkaDependencies: Seq[ModuleID] = Seq(
    akkaActor
  )

  val akkaHttpDependencies: Seq[ModuleID] = Seq(
    akkaHttp
  )


}
